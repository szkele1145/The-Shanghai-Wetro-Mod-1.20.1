package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.Items;
import com.xhg78999.shmetro.blocks.BlockShanghaiPSDTop;
import mtr.block.BlockPSDGlass;
import net.minecraft.world.item.Item;

public class BlockShanghaiPSDGlass
extends BlockPSDGlass {
    public final BlockShanghaiPSDTop.EnumPSDTop top;

    public BlockShanghaiPSDGlass(BlockShanghaiPSDTop.EnumPSDTop top) {
        // MTR BlockPSDGlass(int style) 的 style 只用于它自己的 asItem()
        // 去选 PSD_GLASS_1/2; 本类完全覆写了 asItem()(见下), 该字段不可达。
        // 上游此处写的是占位魔数 114514, 这里改为语义明确的 0, 行为零变化。
        super(0);
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

