package com.xhg78999.shmetro.fabric;

import com.xhg78999.shmetro.Main;
import com.xhg78999.shmetro.fabric.RegistriesWrapperImpl;
import net.fabricmc.api.ModInitializer;

public class MainFabric
implements ModInitializer {
    public void onInitialize() {
        Main.init(new RegistriesWrapperImpl());
    }
}

