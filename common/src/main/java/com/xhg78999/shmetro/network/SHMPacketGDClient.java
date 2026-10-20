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
        // 与服务端 SHMPacketGDServer.MAX_GD_CODE_LENGTH 保持一致。
        // GUI 已用 setMaxLength(16) 限住输入, 这里再截一次是防御性的:
        // 万一有其它调用方绕过 GUI, 也不至于发出一个服务端必然拒绝的包。
        String safeId = id == null ? "" : id;
        if (safeId.length() > SHMPacketDefine.MAX_GD_CODE_LENGTH) {
            safeId = safeId.substring(0, SHMPacketDefine.MAX_GD_CODE_LENGTH);
        }
        packet.writeUtf(safeId);
        RegistryClient.sendToServer((ResourceLocation)SHMPacketDefine.PACKET_SHM_GD_UPDATE, (FriendlyByteBuf)packet);
    }
}

