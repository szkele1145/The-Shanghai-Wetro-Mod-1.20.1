package com.xhg78999.shmetro.network;

import com.xhg78999.shmetro.gui.GuiGlobalPosition;
import com.xhg78999.shmetro.network.SHMPacketDefine;
import io.netty.buffer.Unpooled;
import mtr.RegistryClient;
import mtr.mappings.ScreenMapper;
import mtr.mappings.UtilitiesClient;
import mtr.packet.PacketTrainDataBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

public class SHMPacketGDClient
extends PacketTrainDataBase {
    public static void openSHMGDScreenS2C(Minecraft minecraftClient, FriendlyByteBuf packet) {
        BlockPos pos = packet.readBlockPos();
        minecraftClient.execute(() -> {
            if (!(minecraftClient.screen instanceof GuiGlobalPosition)) {
                UtilitiesClient.setScreen((Minecraft)minecraftClient, (ScreenMapper)new GuiGlobalPosition(pos));
            }
        });
    }

    public static void sendGDUpdateC2S(BlockPos pos, String id) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(pos);
        packet.writeUtf(id);
        RegistryClient.sendToServer((ResourceLocation)SHMPacketDefine.PACKET_SHM_GD_UPDATE, (FriendlyByteBuf)packet);
    }
}

