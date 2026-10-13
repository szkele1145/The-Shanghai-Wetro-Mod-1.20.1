package com.xhg78999.shmetro;

import com.xhg78999.shmetro.Blocks;
import mtr.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.resources.ResourceLocation;

public interface CreativeTab {
    public static final CreativeModeTabs.Wrapper SHNAVIGATE = new CreativeModeTabs.Wrapper(new ResourceLocation("shmetro", "navigation"), () -> new ItemStack((ItemLike)Blocks.BELIEF_BEACON.get()));
    public static final CreativeModeTabs.Wrapper SHFACILITIES = new CreativeModeTabs.Wrapper(new ResourceLocation("shmetro", "facilities"), () -> new ItemStack((ItemLike)Blocks.ERAILING_DOOR_1.get()));
    public static final CreativeModeTabs.Wrapper SHBLOCKS = new CreativeModeTabs.Wrapper(new ResourceLocation("shmetro", "blocks"), () -> new ItemStack((ItemLike)Blocks.TILES_TIC.get()));
}

