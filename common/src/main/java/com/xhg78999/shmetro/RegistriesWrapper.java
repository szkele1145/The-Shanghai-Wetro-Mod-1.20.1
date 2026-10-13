package com.xhg78999.shmetro;

import mtr.CreativeModeTabs;
import mtr.RegistryObject;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.sounds.SoundEvent;

public interface RegistriesWrapper {
    public void registerBlock(String var1, RegistryObject<Block> var2);

    public void registerItem(String var1, RegistryObject<Item> var2, CreativeModeTabs.Wrapper var3);

    public void registerBlockAndItem(String var1, RegistryObject<Block> var2, CreativeModeTabs.Wrapper var3);

    public void registerBlockAndItem(String var1, RegistryObject<Block> var2, CreativeModeTabs.Wrapper var3, String var4);

    public void registerBlockEntityType(String var1, RegistryObject<? extends BlockEntityType<? extends BlockEntityMapper>> var2);

    public void registerEntityType(String var1, RegistryObject<? extends EntityType<? extends Entity>> var2);

    public void registerSoundEvent(String var1, SoundEvent var2);
}

