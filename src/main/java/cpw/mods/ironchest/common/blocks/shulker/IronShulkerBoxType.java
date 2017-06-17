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
package cpw.mods.ironchest.common.blocks.shulker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import cpw.mods.ironchest.common.crafting.IronShulkerBoxRecipe;
import cpw.mods.ironchest.common.gui.shulker.slot.ValidatingShulkerBoxSlot;
import cpw.mods.ironchest.common.tileentity.shulker.TileEntityCopperShulkerBox;
import cpw.mods.ironchest.common.tileentity.shulker.TileEntityCrystalShulkerBox;
import cpw.mods.ironchest.common.tileentity.shulker.TileEntityDiamondShulkerBox;
import cpw.mods.ironchest.common.tileentity.shulker.TileEntityGoldShulkerBox;
import cpw.mods.ironchest.common.tileentity.shulker.TileEntityIronShulkerBox;
import cpw.mods.ironchest.common.tileentity.shulker.TileEntityObsidianShulkerBox;
import cpw.mods.ironchest.common.tileentity.shulker.TileEntitySilverShulkerBox;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraftforge.fml.common.registry.GameRegistry;

public enum IronShulkerBoxType implements IStringSerializable
{
    //@formatter:off
    IRON(54, 9, true, "_iron.png", TileEntityIronShulkerBox.class, 184, 202, Arrays.asList("ingotIron", "ingotRefinedIron"), "mmmmPmmmm", "mGmG3GmGm"),
    GOLD(81, 9, true, "_gold.png", TileEntityGoldShulkerBox.class, 184, 256, Collections.singleton("ingotGold"), "mmmmPmmmm", "mGmG4GmGm"),
    DIAMOND(108, 12, true, "_diamond.png", TileEntityDiamondShulkerBox.class, 184, 256, Collections.singleton("gemDiamond"), "GGGmPmGGG", "GGGG4Gmmm"),
    COPPER(45, 9, false, "_copper.png", TileEntityCopperShulkerBox.class, 184, 184, Collections.singleton("ingotCopper"), "mmmmSmmmm"),
    SILVER(72, 9, false, "_silver.png", TileEntitySilverShulkerBox.class, 184, 238, Collections.singleton("ingotSilver"), "mmmm3mmmm", "mGmG0GmGm"),
    CRYSTAL(108, 12, true, "_crystal.png", TileEntityCrystalShulkerBox.class, 238, 256, Collections.singleton("blockGlass"), "GGGGPGGGG"),
    OBSIDIAN(108, 12, false, "_obsidian.png", TileEntityObsidianShulkerBox.class, 238, 256, Collections.singleton("obsidian"), "mmmm2mmmm"),
    VANILLA(0, 0, false, "", null, 0, 0, Collections.singleton("shulkerBox"));
    //@formatter:on

    public static final IronShulkerBoxType VALUES[] = values();

    public final String name;

    public final int size;

    public final int rowLength;

    public final boolean tieredShulkerBox;

    public final String modelTexture;

    public final Class<? extends TileEntityIronShulkerBox> clazz;

    public final int xSize;

    public final int ySize;

    public final Collection<String> matList;

    public final Collection<String> recipes;

    private String breakTexture;

    //@formatter:off
    IronShulkerBoxType(int size, int rowLength, boolean tieredShulkerBox, String modelTexture, Class<? extends TileEntityIronShulkerBox> clazz, int xSize, int ySize, Collection<String> mats, String... recipes)
    //@formatter:on
    {
        this.name = this.name().toLowerCase();
        this.size = size;
        this.rowLength = rowLength;
        this.tieredShulkerBox = tieredShulkerBox;
        this.modelTexture = modelTexture;
        this.clazz = clazz;
        this.xSize = xSize;
        this.ySize = ySize;
        this.matList = Collections.unmodifiableCollection(mats);
        this.recipes = Collections.unmodifiableCollection(Arrays.asList(recipes));
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    public String getBreakTexture()
    {
        if (this.breakTexture == null)
        {
            switch (this)
            {
            case OBSIDIAN:
            {
                this.breakTexture = "minecraft:blocks/obsidian";
                break;
            }
            case VANILLA:
            {
                this.breakTexture = "minecraft:blocks/planks_oak";
                break;
            }
            default:
            {
                this.breakTexture = "ironchest:blocks/" + this.getName() + "break";
            }
            }
        }

        return this.breakTexture;
    }

    public static void registerBlocksAndRecipes(BlockIronShulkerBox blockResult, BlockShulkerBox shulkerBox)
    {
        Object previous = shulkerBox;

        for (IronShulkerBoxType typ : values())
        {
            generateRecipesForType(blockResult, previous, typ, shulkerBox);

            ItemStack newShulkerBox = new ItemStack(blockResult, 1, typ.ordinal());

            if (typ.tieredShulkerBox)
            {
                previous = newShulkerBox;
            }
        }
    }

    public static void generateRecipesForType(BlockIronShulkerBox blockResult, Object previousTier, IronShulkerBoxType type, BlockShulkerBox shulkerBox)
    {
        for (String recipe : type.recipes)
        {
            String[] recipeSplit = new String[] { recipe.substring(0, 3), recipe.substring(3, 6), recipe.substring(6, 9) };
            Object mainMaterial = null;

            for (String mat : type.matList)
            {
                mainMaterial = translateOreName(mat);
                //@formatter:off
                addRecipe(new ItemStack(blockResult, 1, type.ordinal()), recipeSplit,
                        'm', mainMaterial,
                        'P', previousTier, /* previous tier of shulker box */
                        'G', "blockGlass",
                        'S', shulkerBox,
                        '0', new ItemStack(blockResult, 1, 0), /* Iron Shulker Box */
                        '1', new ItemStack(blockResult, 1, 1), /* Gold Shulker Box */
                        '2', new ItemStack(blockResult, 1, 2), /* Diamond Shulker Box */
                        '3', new ItemStack(blockResult, 1, 3), /* Copper Shulker Box */
                        '4', new ItemStack(blockResult, 1, 4) /* Silver Shulker Box */
                        );
                //@formatter:on
            }
        }
    }

    public static Object translateOreName(String mat)
    {
        if (mat.equals("shulkerBox"))
        {
            return Blocks.PURPLE_SHULKER_BOX;
        }

        if (mat.equals("obsidian"))
        {
            return Blocks.OBSIDIAN;
        }

        return mat;
    }

    public static void addRecipe(ItemStack is, Object... parts)
    {
        IronShulkerBoxRecipe oreRecipe = new IronShulkerBoxRecipe(is, parts);

        GameRegistry.addRecipe(oreRecipe);
    }

    public int getRowCount()
    {
        return this.size / this.rowLength;
    }

    public boolean isTransparent()
    {
        return this == CRYSTAL;
    }

    public boolean isValidForCreativeMode()
    {
        return this != VANILLA;
    }

    public boolean isExplosionResistant()
    {
        return this == OBSIDIAN;
    }

    public Slot makeSlot(IInventory chestInventory, int index, int x, int y)
    {
        return new ValidatingShulkerBoxSlot(chestInventory, index, x, y);
    }

    public TileEntityIronShulkerBox makeEntity(EnumDyeColor colorIn)
    {
        switch (this)
        {
        case IRON:
            return new TileEntityIronShulkerBox(colorIn);
        case GOLD:
            return new TileEntityGoldShulkerBox(colorIn);
        case DIAMOND:
            return new TileEntityDiamondShulkerBox(colorIn);
        case COPPER:
            return new TileEntityCopperShulkerBox(colorIn);
        case SILVER:
            return new TileEntitySilverShulkerBox(colorIn);
        case CRYSTAL:
            return new TileEntityCrystalShulkerBox(colorIn);
        case OBSIDIAN:
            return new TileEntityObsidianShulkerBox(colorIn);
        default:
            return null;
        }
    }
}
