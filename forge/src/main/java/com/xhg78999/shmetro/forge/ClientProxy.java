package com.xhg78999.shmetro.forge;

import com.xhg78999.shmetro.MainClient;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientProxy {
   public static class ForgeEventBusListener {
   }

   public static class ModEventBusListener {
      @SubscribeEvent
      public static void onClientSetupEvent(FMLClientSetupEvent event) {
         MainClient.init();
      }
   }
}
