package com.xhg78999.shmetro.forge;

import com.xhg78999.shmetro.RegistriesWrapper;
import com.xhg78999.shmetro.items.ItemBlockTooltipBase;
import mtr.Registry;
import mtr.RegistryObject;
import mtr.CreativeModeTabs.Wrapper;
import mtr.item.ItemWithCreativeTabBase;
import mtr.item.ItemWithCreativeTabBase.ItemPlaceOnWater;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.DeferredRegisterHolder;
import mtr.mappings.RegistryUtilities;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RegistriesWrapperImpl implements RegistriesWrapper {
   private static final DeferredRegisterHolder<Item> ITEMS = new DeferredRegisterHolder("shmetro", Registries.ITEM);
   private static final DeferredRegisterHolder<Block> BLOCKS = new DeferredRegisterHolder("shmetro", Registries.BLOCK);
   private static final DeferredRegisterHolder<BlockEntityType<?>> BLOCK_ENTITY_TYPES = new DeferredRegisterHolder("shmetro", Registries.BLOCK_ENTITY_TYPE);
   private static final DeferredRegisterHolder<EntityType<?>> ENTITY_TYPES = new DeferredRegisterHolder("shmetro", Registries.ENTITY_TYPE);
   private static final DeferredRegisterHolder<SoundEvent> SOUND_EVENTS = new DeferredRegisterHolder("shmetro", Registries.SOUND_EVENT);

   @Override
   public void registerBlock(String id, RegistryObject<Block> block) {
      BLOCKS.register(id, block::get);
   }

   @Override
   public void registerBlockAndItem(String id, RegistryObject<Block> block, Wrapper tab) {
      BLOCKS.register(id, block::get);
      ITEMS.register(id, () -> {
         BlockItem blockItem = new BlockItem(block.get(), RegistryUtilities.createItemProperties(tab::get));
         Registry.registerCreativeModeTab(tab.resourceLocation, blockItem);
         return blockItem;
      });
   }

   @Override
   public void registerBlockAndItem(String id, RegistryObject<Block> block, Wrapper tab, String tooltip) {
      BLOCKS.register(id, block::get);
      ITEMS.register(id, () -> {
         ItemBlockTooltipBase blockItem = new ItemBlockTooltipBase(block.get(), RegistryUtilities.createItemProperties(tab::get), tooltip);
         Registry.registerCreativeModeTab(tab.resourceLocation, blockItem);
         return blockItem;
      });
   }

   @Override
   public void registerItem(String id, RegistryObject<Item> item, Wrapper tab) {
      ITEMS.register(id, () -> {
         Item itemObject = item.get();
         if (itemObject instanceof ItemWithCreativeTabBase) {
            Registry.registerCreativeModeTab(((ItemWithCreativeTabBase)itemObject).creativeModeTab.resourceLocation, itemObject);
         } else if (itemObject instanceof ItemPlaceOnWater) {
            Registry.registerCreativeModeTab(((ItemPlaceOnWater)itemObject).creativeModeTab.resourceLocation, itemObject);
         }

         return itemObject;
      });
   }

   @Override
   public void registerBlockEntityType(String id, RegistryObject<? extends BlockEntityType<? extends BlockEntityMapper>> blockEntityType) {
      BLOCK_ENTITY_TYPES.register(id, blockEntityType::get);
   }

   @Override
   public void registerEntityType(String id, RegistryObject<? extends EntityType<? extends Entity>> entityType) {
      ENTITY_TYPES.register(id, entityType::get);
   }

   @Override
   public void registerSoundEvent(String id, SoundEvent soundEvent) {
      SOUND_EVENTS.register(id, () -> soundEvent);
   }

   public void registerAllDeferred() {
      ITEMS.register();
      BLOCKS.register();
      BLOCK_ENTITY_TYPES.register();
      ENTITY_TYPES.register();
      SOUND_EVENTS.register();
   }
}
