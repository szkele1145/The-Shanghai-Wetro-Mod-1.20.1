package com.xhg78999.shmetro.items;

import java.util.List;
import mtr.mappings.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class ItemBlockTooltipBase
extends BlockItem {
    private final String tooltip;

    public ItemBlockTooltipBase(Block block, Item.Properties properties, String tooltip) {
        super(block, properties);
        this.tooltip = tooltip;
    }

    public void appendHoverText(ItemStack itemStack, Level level, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add((Component)Text.translatable((String)this.tooltip, (Object[])new Object[0]).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
    }
}

