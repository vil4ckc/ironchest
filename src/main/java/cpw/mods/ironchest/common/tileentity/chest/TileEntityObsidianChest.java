package cpw.mods.ironchest.common.tileentity.chest;

import cpw.mods.ironchest.common.blocks.chest.IronChestType;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.walkers.ItemStackDataLists;

public class TileEntityObsidianChest extends TileEntityIronChest
{
    public TileEntityObsidianChest()
    {
        super(IronChestType.OBSIDIAN);
    }

    public static void registerFixesChest(DataFixer fixer)
    {
        fixer.registerWalker(FixTypes.BLOCK_ENTITY, new ItemStackDataLists(TileEntityObsidianChest.class, new String[] { "Items" }));
    }
}
