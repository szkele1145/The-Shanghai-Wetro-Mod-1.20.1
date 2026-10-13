package com.xhg78999.shmetro.fabric;

import com.xhg78999.shmetro.MainClient;
import net.fabricmc.api.ClientModInitializer;

public class MainFabricClient
implements ClientModInitializer {
    public void onInitializeClient() {
        MainClient.init();
    }
}

