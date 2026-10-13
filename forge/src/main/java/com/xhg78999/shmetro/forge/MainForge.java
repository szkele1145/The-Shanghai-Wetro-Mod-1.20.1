package com.xhg78999.shmetro.forge;

import com.xhg78999.shmetro.Main;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import dev.architectury.platform.forge.EventBuses;

@Mod("shmetro")
public class MainForge {
   private static final RegistriesWrapperImpl registries = new RegistriesWrapperImpl();

   public MainForge() {
      IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
      EventBuses.registerModEventBus("shmetro", eventBus);
      registries.registerAllDeferred();
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            eventBus.register(ClientProxy.ModEventBusListener.class);
            MinecraftForge.EVENT_BUS.register(ClientProxy.ForgeEventBusListener.class);
         });
   }

   static {
      Main.init(registries);
   }
}
