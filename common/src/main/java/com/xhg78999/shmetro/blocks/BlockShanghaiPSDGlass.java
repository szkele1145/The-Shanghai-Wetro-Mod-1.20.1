package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.Items;
import com.xhg78999.shmetro.blocks.BlockShanghaiPSDTop;
import mtr.block.BlockPSDGlass;
import net.minecraft.world.item.Item;

public class BlockShanghaiPSDGlass
extends BlockPSDGlass {
    public final BlockShanghaiPSDTop.EnumPSDTop top;

    public BlockShanghaiPSDGlass(BlockShanghaiPSDTop.EnumPSDTop top) {
        super(114514);
        this.top = top;
    }

    public Item asItem() {
        return switch (this.top) {
            case RAILMAP -> (Item)Items.SH_PSD_GLASS_RAILMAP.get();
            case DESTINATION -> (Item)Items.SH_PSD_GLASS_DESTINATION.get();
            default -> null;
        };
    }
}

