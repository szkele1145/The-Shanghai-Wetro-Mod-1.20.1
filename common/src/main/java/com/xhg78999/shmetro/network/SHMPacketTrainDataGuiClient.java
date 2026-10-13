package com.xhg78999.shmetro.network;

import com.xhg78999.shmetro.gui.GuiShanghaiSign;
import com.xhg78999.shmetro.network.SHMPacketDefine;
import io.netty.buffer.Unpooled;
import java.util.Set;
import mtr.RegistryClient;
import mtr.mappings.ScreenMapper;
import mtr.mappings.UtilitiesClient;
import mtr.packet.PacketTrainDataBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

public class SHMPacketTrainDataGuiClient
extends PacketTrainDataBase {
    public static void openSHMSignScreenS2C(Minecraft minecraftClient, FriendlyByteBuf packet) {
        BlockPos pos = packet.readBlockPos();
        minecraftClient.execute(() -> {
            if (!(minecraftClient.screen instanceof GuiShanghaiSign)) {
                UtilitiesClient.setScreen((Minecraft)minecraftClient, (ScreenMapper)new GuiShanghaiSign(pos));
            }
        });
    }

    public static void sendSHMSignIdsC2S(BlockPos signPos, Set<Long> selectedIds, String[] signIds) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(signPos);
        packet.writeInt(selectedIds.size());
        selectedIds.forEach(arg_0 -> ((FriendlyByteBuf)packet).writeLong(arg_0));
        packet.writeInt(signIds.length);
        for (String signType : signIds) {
            packet.writeUtf(signType == null ? "" : signType);
        }
        RegistryClient.sendToServer((ResourceLocation)SHMPacketDefine.PACKET_SHM_SIGN_TYPES, (FriendlyByteBuf)packet);
    }
}

