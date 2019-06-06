/*******************************************************************************
 * Copyright (c) 2012 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p>
 * Contributors:
 * cpw - initial API and implementation
 ******************************************************************************/
package cpw.mods.ironchest.common.tileentity.chest;

import cpw.mods.ironchest.common.blocks.chest.BlockIronChest;
import cpw.mods.ironchest.common.blocks.chest.IronChestType;
import cpw.mods.ironchest.common.core.IronChestBlocks;
import cpw.mods.ironchest.common.gui.chest.ContainerIronChest;
import cpw.mods.ironchest.common.lib.ICChestInventoryHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.walkers.ItemStackDataLists;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class TileEntityIronChest extends TileEntityLockableLoot implements ITickable
{
    /** Chest Contents */
    public NonNullList<ItemStack> chestContents;

    /** The current angle of the lid (between 0 and 1) */
    public float lidAngle;

    /** The angle of the lid last tick */
    public float prevLidAngle;

    /** The number of players currently using this chest */
    public int numPlayersUsing;

    /** Server sync counter (once per 20 ticks) */
    private int ticksSinceSync;

    /** Direction chest is facing */
    private EnumFacing facing;

    private String customName;

    private IronChestType chestType;

    public TileEntityIronChest()
    {
        this(IronChestType.IRON);
    }

    protected TileEntityIronChest(IronChestType type)
    {
        super();
        this.chestType = type;
        this.chestContents = NonNullList.withSize(type.size, ItemStack.EMPTY);
        this.facing = EnumFacing.NORTH;
    }

    public void setContents(NonNullList<ItemStack> contents)
    {
        this.chestContents = NonNullList.withSize(this.getType().size, ItemStack.EMPTY);

        for (int i = 0; i < contents.size(); i++)
        {
            if (i < this.chestContents.size())
            {
                this.getItems().set(i, contents.get(i));
            }
        }
    }

    @Override
    public int getSizeInventory()
    {
        return this.getItems().size();
    }

    public EnumFacing getFacing()
    {
        return this.facing;
    }

    public IronChestType getType()
    {
        IronChestType type = IronChestType.IRON;

        if (this.hasWorld())
        {
            IBlockState state = this.world.getBlockState(this.pos);

            if (state.getBlock() == IronChestBlocks.ironChestBlock)
            {
                type = state.getValue(BlockIronChest.VARIANT_PROP);
            }
        }

        return type;
    }

    @Override
    public ItemStack getStackInSlot(int index)
    {
        this.fillWithLoot(null);

        return this.getItems().get(index);
    }

    @Override
    public void markDirty()
    {
        super.markDirty();
    }

    @Override
    public String getName()
    {
        return this.hasCustomName() ? this.customName : this.getType().name();
    }

    @Override
    public boolean hasCustomName()
    {
        return this.customName != null && this.customName.length() > 0;
    }

    @Override
    public void setCustomName(String name)
    {
        this.customName = name;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        this.chestContents = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);

        if (compound.hasKey("CustomName", Constants.NBT.TAG_STRING))
        {
            this.customName = compound.getString("CustomName");
        }

        if (!this.checkLootAndRead(compound))
        {
            ItemStackHelper.loadAllItems(compound, this.chestContents);
        }

        this.facing = EnumFacing.VALUES[compound.getByte("facing")];
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);

        if (!this.checkLootAndWrite(compound))
        {
            ItemStackHelper.saveAllItems(compound, this.chestContents);
        }

        compound.setByte("facing", (byte) this.facing.ordinal());

        if (this.hasCustomName())
        {
            compound.setString("CustomName", this.customName);
        }

        return compound;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player)
    {
        if (this.world == null)
        {
            return true;
        }

        if (this.world.getTileEntity(this.pos) != this)
        {
            return false;
        }

        return player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64D;
    }

    @Override
    public void update()
    {
        int x = this.pos.getX();
        int y = this.pos.getY();
        int z = this.pos.getZ();
        ++this.ticksSinceSync;

        // Resynchronizes clients with the server state
        if (this.world != null && !this.world.isRemote && this.numPlayersUsing != 0 && (this.ticksSinceSync + x + y + z) % 200 == 0)
        {
            this.numPlayersUsing = 0;

            for (EntityPlayer player : this.world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(x - 5.0F, y - 5.0F, z - 5.0F, x + 1 + 5.0F, y + 1 + 5.0F, z + 1 + 5.0F)))
            {
                if (player.openContainer instanceof ContainerIronChest)
                {
                    ++this.numPlayersUsing;
                }
            }
        }

        if (this.world != null && !this.world.isRemote && this.ticksSinceSync < 0)
        {
            this.world.addBlockEvent(this.pos, IronChestBlocks.ironChestBlock, 3, ((this.numPlayersUsing << 3) & 0xF8) | (this.facing.ordinal() & 0x7));
        }

        this.prevLidAngle = this.lidAngle;
        float angle = 0.1F;

        if (this.numPlayersUsing > 0 && this.lidAngle == 0.0F)
        {
            double soundX = x + 0.5D;
            double soundY = y + 0.5D;
            double soundZ = z + 0.5D;

            this.world.playSound(null, soundX, soundY, soundZ, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F);
        }

        if (this.numPlayersUsing == 0 && this.lidAngle > 0.0F || this.numPlayersUsing > 0 && this.lidAngle < 1.0F)
        {
            float currentAngle = this.lidAngle;

            if (this.numPlayersUsing > 0)
            {
                this.lidAngle += angle;
            }
            else
            {
                this.lidAngle -= angle;
            }

            if (this.lidAngle > 1.0F)
            {
                this.lidAngle = 1.0F;
            }

            float maxAngle = 0.5F;

            if (this.lidAngle < maxAngle && currentAngle >= maxAngle)
            {
                double soundX = x + 0.5D;
                double soundY = y + 0.5D;
                double soundZ = z + 0.5D;

                this.world.playSound(null, soundX, soundY, soundZ, SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F);
            }

            if (this.lidAngle < 0.0F)
            {
                this.lidAngle = 0.0F;
            }
        }
    }

    @Override
    public boolean receiveClientEvent(int id, int type)
    {
        if (id == 1)
        {
            this.numPlayersUsing = type;
        }
        else if (id == 2)
        {
            this.facing = EnumFacing.VALUES[type];
        }
        else if (id == 3)
        {
            this.facing = EnumFacing.VALUES[type & 0x7];
            this.numPlayersUsing = (type & 0xF8) >> 3;
        }

        return super.receiveClientEvent(id, type);
    }

    @Override
    public void openInventory(EntityPlayer player)
    {
        if (!player.isSpectator())
        {
            if (this.numPlayersUsing < 0)
            {
                this.numPlayersUsing = 0;
            }

            ++this.numPlayersUsing;

            this.world.addBlockEvent(this.pos, IronChestBlocks.ironChestBlock, 1, this.numPlayersUsing);
            this.world.notifyNeighborsOfStateChange(this.pos, IronChestBlocks.ironChestBlock, false);
        }
    }

    @Override
    public void closeInventory(EntityPlayer player)
    {
        if (!player.isSpectator())
        {
            --this.numPlayersUsing;

            this.world.addBlockEvent(this.pos, IronChestBlocks.ironChestBlock, 1, this.numPlayersUsing);
            this.world.notifyNeighborsOfStateChange(this.pos, IronChestBlocks.ironChestBlock, false);
        }
    }

    public void setFacing(EnumFacing facing)
    {
        this.facing = facing;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        NBTTagCompound compound = new NBTTagCompound();

        compound.setByte("facing", (byte) this.facing.ordinal());

        return new SPacketUpdateTileEntity(this.pos, 0, compound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
    {
        if (pkt.getTileEntityType() == 0)
        {
            NBTTagCompound compound = pkt.getNbtCompound();

            this.facing = EnumFacing.VALUES[compound.getByte("facing")];
        }
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    {
        return this.getType().acceptsStack(stack);
    }

    public void rotateAround()
    {
        this.setFacing(this.facing.rotateY());

        this.world.addBlockEvent(this.pos, IronChestBlocks.ironChestBlock, 2, this.facing.ordinal());
    }

    public void wasPlaced(EntityLivingBase entityliving, ItemStack stack)
    {
    }

    public void removeAdornments()
    {
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn)
    {
        this.fillWithLoot(playerIn);

        return new ContainerIronChest(playerInventory, this, this.chestType, this.chestType.xSize, this.chestType.ySize);
    }

    @Override
    public String getGuiID()
    {
        return "IronChest:" + this.getType().name() + "_chest";
    }

    @Override
    public boolean canRenderBreaking()
    {
        return true;
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        NBTTagCompound compound = super.getUpdateTag();

        if (!this.checkLootAndWrite(compound))
        {
            ItemStackHelper.saveAllItems(compound, this.chestContents);
        }

        compound.setByte("facing", (byte) this.facing.ordinal());

        if (this.hasCustomName())
        {
            compound.setString("CustomName", this.customName);
        }

        return compound;
    }

    @Override
    public NonNullList<ItemStack> getItems()
    {
        return this.chestContents;
    }

    @Override
    public boolean isEmpty()
    {
        for (ItemStack itemstack : this.chestContents)
        {
            if (!itemstack.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    public static void registerFixesChest(DataFixer fixer)
    {
        fixer.registerWalker(FixTypes.BLOCK_ENTITY, new ItemStackDataLists(TileEntityIronChest.class, "Items"));
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    private IItemHandler itemHandler;

    @Override
    protected IItemHandler createUnSidedHandler()
    {
        return new ICChestInventoryHandler(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return (T) (this.itemHandler == null ? (this.itemHandler = this.createUnSidedHandler()) : this.itemHandler);
        }
        return super.getCapability(capability, facing);
    }
}
