package com.xhg78999.shmetro.fabric;

import com.xhg78999.shmetro.RegistriesWrapper;
import com.xhg78999.shmetro.items.ItemBlockTooltipBase;
import mtr.mappings.FabricRegistryUtilities;
import mtr.CreativeModeTabs;
import mtr.RegistryObject;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.RegistryUtilities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class RegistriesWrapperImpl
implements RegistriesWrapper {
    @Override
    public void registerBlock(String id, RegistryObject<Block> block) {
        Registry.register((Registry)RegistryUtilities.registryGetBlock(), (ResourceLocation)new ResourceLocation("shmetro", id), (Object)((Block)block.get()));
    }

    @Override
    public void registerBlockAndItem(String id, RegistryObject<Block> block, CreativeModeTabs.Wrapper tab) {
        Registry.register((Registry)RegistryUtilities.registryGetBlock(), (ResourceLocation)new ResourceLocation("shmetro", id), (Object)((Block)block.get()));
        BlockItem blockItem = new BlockItem((Block)block.get(), RegistryUtilities.createItemProperties(() -> ((CreativeModeTabs.Wrapper)tab).get()));
        Registry.register((Registry)RegistryUtilities.registryGetItem(), (ResourceLocation)new ResourceLocation("shmetro", id), (Object)blockItem);
        FabricRegistryUtilities.registerCreativeModeTab(tab.get(), (Item)blockItem);
    }

    @Override
    public void registerBlockAndItem(String id, RegistryObject<Block> block, CreativeModeTabs.Wrapper tab, String tooltip) {
        Registry.register((Registry)RegistryUtilities.registryGetBlock(), (ResourceLocation)new ResourceLocation("shmetro", id), (Object)((Block)block.get()));
        ItemBlockTooltipBase blockItem = new ItemBlockTooltipBase((Block)block.get(), RegistryUtilities.createItemProperties(() -> ((CreativeModeTabs.Wrapper)tab).get()), tooltip);
        Registry.register((Registry)RegistryUtilities.registryGetItem(), (ResourceLocation)new ResourceLocation("shmetro", id), (Object)((Object)blockItem));
        FabricRegistryUtilities.registerCreativeModeTab(tab.get(), (Item)blockItem);
    }

    @Override
    public void registerItem(String id, RegistryObject<Item> item, CreativeModeTabs.Wrapper tab) {
        Registry.register((Registry)RegistryUtilities.registryGetItem(), (ResourceLocation)new ResourceLocation("shmetro", id), (Object)((Item)item.get()));
        FabricRegistryUtilities.registerCreativeModeTab(tab.get(), (Item)item.get());
    }

    @Override
    public void registerBlockEntityType(String id, RegistryObject<? extends BlockEntityType<? extends BlockEntityMapper>> blockEntityType) {
        Registry.register((Registry)RegistryUtilities.registryGetBlockEntityType(), (ResourceLocation)new ResourceLocation("shmetro", id), (Object)((BlockEntityType)blockEntityType.get()));
    }

    @Override
    public void registerEntityType(String id, RegistryObject<? extends EntityType<? extends Entity>> entityType) {
        Registry.register((Registry)RegistryUtilities.registryGetEntityType(), (ResourceLocation)new ResourceLocation("shmetro", id), (Object)((EntityType)entityType.get()));
    }

    @Override
    public void registerSoundEvent(String id, SoundEvent soundEvent) {
        Registry.register((Registry)RegistryUtilities.registryGetSoundEvent(), (ResourceLocation)new ResourceLocation("shmetro", id), (Object)soundEvent);
    }
}

