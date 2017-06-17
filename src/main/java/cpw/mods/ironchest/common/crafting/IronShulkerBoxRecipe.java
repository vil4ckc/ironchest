package cpw.mods.ironchest.common.crafting;

import javax.annotation.Nonnull;

import cpw.mods.ironchest.common.blocks.shulker.BlockIronShulkerBox;
import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class IronShulkerBoxRecipe extends ShapedOreRecipe
{
    public IronShulkerBoxRecipe(@Nonnull ItemStack result, Object... recipe)
    {
        super(result, recipe);
    }

    @Override
    @Nonnull
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting var1)
    {
        ItemStack newOutput = this.output.copy();

        ItemStack itemstack = ItemStack.EMPTY;

        for (int i = 0; i < var1.getSizeInventory(); ++i)
        {
            ItemStack stack = var1.getStackInSlot(i);

            if (!stack.isEmpty())
            {
                if (Block.getBlockFromItem(stack.getItem()) instanceof BlockIronShulkerBox
                        || Block.getBlockFromItem(stack.getItem()) instanceof BlockShulkerBox)
                {
                    itemstack = stack;
                }
            }
        }

        if (itemstack.hasTagCompound())
        {
            newOutput.setTagCompound(itemstack.getTagCompound().copy());
        }

        return newOutput;
    }
}
