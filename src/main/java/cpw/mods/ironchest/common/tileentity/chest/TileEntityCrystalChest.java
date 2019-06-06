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

import cpw.mods.ironchest.IronChest;
import cpw.mods.ironchest.common.blocks.chest.IronChestType;
import cpw.mods.ironchest.common.network.MessageCrystalChestSync;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.walkers.ItemStackDataLists;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.Collections;

public class TileEntityCrystalChest extends TileEntityIronChest
{
    /** Crystal Chest top stacks */
    private NonNullList<ItemStack> topStacks;

    /** If the inventory got touched */
    private boolean inventoryTouched;

    /** If the inventory had items */
    private boolean hadStuff;

    public TileEntityCrystalChest()
    {
        super(IronChestType.CRYSTAL);
        this.topStacks = NonNullList.<ItemStack>withSize(8, ItemStack.EMPTY);
    }

    public NonNullList<ItemStack> getTopItems()
    {
        return this.topStacks;
    }

    @Override
    public void setContents(NonNullList<ItemStack> contents)
    {
        super.setContents(contents);

        this.inventoryTouched = true;
    }

    @Override
    public ItemStack getStackInSlot(int index)
    {
        this.fillWithLoot((EntityPlayer) null);

        this.inventoryTouched = true;

        return this.getItems().get(index);
    }

    @Override
    public void markDirty()
    {
        super.markDirty();

        this.sortTopStacks();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        this.sortTopStacks();
    }

    @Override
    public void update()
    {
        super.update();

        if (!this.world.isRemote && this.inventoryTouched)
        {
            this.inventoryTouched = false;

            this.sortTopStacks();
        }
    }

    protected void sortTopStacks()
    {
        if (!this.getType().isTransparent() || (this.world != null && this.world.isRemote))
        {
            return;
        }

        NonNullList<ItemStack> tempCopy = NonNullList.<ItemStack>withSize(this.getSizeInventory(), ItemStack.EMPTY);

        boolean hasStuff = false;

        int compressedIdx = 0;

        mainLoop:
        for (int i = 0; i < this.getSizeInventory(); i++)
        {
            ItemStack itemStack = this.getItems().get(i);

            if (!itemStack.isEmpty())
            {
                for (int j = 0; j < compressedIdx; j++)
                {
                    ItemStack tempCopyStack = tempCopy.get(j);

                    if (ItemStack.areItemsEqualIgnoreDurability(tempCopyStack, itemStack))
                    {
                        if (itemStack.getCount() != tempCopyStack.getCount())
                        {
                            tempCopyStack.grow(itemStack.getCount());
                        }

                        continue mainLoop;
                    }
                }

                tempCopy.set(compressedIdx, itemStack.copy());

                compressedIdx++;

                hasStuff = true;
            }
        }

        if (!hasStuff && this.hadStuff)
        {
            this.hadStuff = false;

            for (int i = 0; i < this.getTopItems().size(); i++)
            {
                this.getTopItems().set(i, ItemStack.EMPTY);
            }

            if (this.world != null)
            {
                IBlockState iblockstate = this.world.getBlockState(this.pos);

                this.world.notifyBlockUpdate(this.pos, iblockstate, iblockstate, 3);
            }

            return;
        }

        this.hadStuff = true;

        Collections.sort(tempCopy, (stack1, stack2) -> {
            if (stack1.isEmpty())
            {
                return 1;
            }
            else if (stack2.isEmpty())
            {
                return -1;
            }
            else
            {
                return stack2.getCount() - stack1.getCount();
            }
        });

        int p = 0;

        for (ItemStack element : tempCopy)
        {
            if (!element.isEmpty() && element.getCount() > 0)
            {
                if (p == this.getTopItems().size())
                {
                    break;
                }

                this.getTopItems().set(p, element);

                p++;
            }
        }

        for (int i = p; i < this.getTopItems().size(); i++)
        {
            this.getTopItems().set(i, ItemStack.EMPTY);
        }

        if (this.world != null)
        {
            IBlockState iblockstate = this.world.getBlockState(this.pos);

            this.world.notifyBlockUpdate(this.pos, iblockstate, iblockstate, 3);
        }

        this.sendTopStacksPacket();
    }

    public NonNullList<ItemStack> buildItemStackDataList()
    {
        if (this.getType().isTransparent())
        {
            NonNullList<ItemStack> sortList = NonNullList.<ItemStack>withSize(this.getTopItems().size(), ItemStack.EMPTY);

            int pos = 0;

            for (ItemStack is : this.topStacks)
            {
                if (!is.isEmpty())
                {
                    sortList.set(pos, is);
                }
                else
                {
                    sortList.set(pos, ItemStack.EMPTY);
                }

                pos++;
            }

            return sortList;
        }

        return NonNullList.<ItemStack>withSize(this.getTopItems().size(), ItemStack.EMPTY);
    }

    protected void sendTopStacksPacket()
    {
        NonNullList<ItemStack> stacks = this.buildItemStackDataList();
        IronChest.packetHandler.sendToAllAround(new MessageCrystalChestSync(this.getWorld().provider.getDimension(), this.getPos(), stacks), new NetworkRegistry.TargetPoint(world.provider.getDimension(), getPos().getX(), getPos().getY(), getPos().getZ(), 128));
    }

    public void receiveMessageFromServer(NonNullList<ItemStack> topStacks)
    {
        this.topStacks = topStacks;
    }

    public static void registerFixesChest(DataFixer fixer)
    {
        fixer.registerWalker(FixTypes.BLOCK_ENTITY, new ItemStackDataLists(TileEntityCrystalChest.class, new String[] { "Items" }));
    }
}
