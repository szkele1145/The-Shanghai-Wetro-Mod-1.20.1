package com.xhg78999.shmetro;

import com.xhg78999.shmetro.items.ItemShanghaiPSD;
import com.xhg78999.shmetro.items.ItemShanghaiRailingDoor;
import com.xhg78999.shmetro.items.ItemShanghaiRailingDoorGlass;
import mtr.RegistryObject;
import net.minecraft.world.item.Item;

public interface Items {
    public static final RegistryObject<Item> ERAILING_DOOR_1 = new RegistryObject(() -> new ItemShanghaiRailingDoor(1));
    public static final RegistryObject<Item> ERAILING_DOOR_2 = new RegistryObject(() -> new ItemShanghaiRailingDoor(2));
    public static final RegistryObject<Item> ERAILING_GLASS_1 = new RegistryObject(() -> new ItemShanghaiRailingDoorGlass(1));
    public static final RegistryObject<Item> ERAILING_GLASS_2 = new RegistryObject(() -> new ItemShanghaiRailingDoorGlass(2));
    public static final RegistryObject<Item> SH_PSD_GLASS_DESTINATION = new RegistryObject(() -> new ItemShanghaiPSD(0));
    public static final RegistryObject<Item> SH_PSD_GLASS_RAILMAP = new RegistryObject(() -> new ItemShanghaiPSD(1));
    public static final RegistryObject<Item> SH_PSD_GLASS_BLANK = new RegistryObject(() -> new ItemShanghaiPSD(19));
    public static final RegistryObject<Item> SH_PSD_DOOR_RUNIN = new RegistryObject(() -> new ItemShanghaiPSD(20));
    public static final RegistryObject<Item> SH_PSD_DOOR_RAILMAP = new RegistryObject(() -> new ItemShanghaiPSD(21));
    public static final RegistryObject<Item> SH_PSD_DOOR_BLANK = new RegistryObject(() -> new ItemShanghaiPSD(22));
    public static final RegistryObject<Item> SH_PSD_END = new RegistryObject(() -> new ItemShanghaiPSD(10));
}

