package com.xhg78999.shmetro.network;

import com.xhg78999.shmetro.entity.BlockEntityGlobalPosition;
import mtr.Registry;
import mtr.packet.PacketTrainDataBase;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;

public class SHMPacketGDServer
extends PacketTrainDataBase {
    public static void openSHMGDScreenS2C(ServerPlayer player, BlockPos signPos) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(signPos);
        Registry.sendToPlayer((ServerPlayer)player, (ResourceLocation)SHMPacketDefine.PACKET_SHM_OPEN_GD_SCREEN, (FriendlyByteBuf)packet);
    }

    public static void receiveSHMGDUpdateC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        BlockPos gdPos = packet.readBlockPos();
        String gdId = packet.readUtf();
        minecraftServer.execute(() -> {
            BlockEntity entity = player.serverLevel().getBlockEntity(gdPos);
            if (entity instanceof BlockEntityGlobalPosition) {
                SHMNetUtils.setTileEntityDataAndWriteUpdate(player, entity2 -> {
                    entity2.code = gdId;
                    entity2.syncData();
                }, new BlockEntityGlobalPosition[]{(BlockEntityGlobalPosition) entity});
            }
        });
    }
}
