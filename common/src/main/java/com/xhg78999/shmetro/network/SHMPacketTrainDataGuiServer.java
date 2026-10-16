package com.xhg78999.shmetro.network;

import com.xhg78999.shmetro.blocks.BlockShanghaiSign;
import mtr.Registry;
import mtr.block.BlockRouteSignBase;
import mtr.packet.PacketTrainDataBase;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;

public class SHMPacketTrainDataGuiServer
extends PacketTrainDataBase {
    public static void openSHMSignScreenS2C(ServerPlayer player, BlockPos signPos) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(signPos);
        Registry.sendToPlayer((ServerPlayer)player, (ResourceLocation)SHMPacketDefine.PACKET_OPEN_SHM_SIGN_SCREEN, (FriendlyByteBuf)packet);
    }

    public static void receiveSHMSignIdsC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        BlockPos signPos = packet.readBlockPos();
        int selectedIdsLength = packet.readInt();
        java.util.HashSet<Long> selectedIds = new java.util.HashSet<Long>();
        for (int i = 0; i < selectedIdsLength; ++i) {
            selectedIds.add(packet.readLong());
        }
        int signLength = packet.readInt();
        String[] signIds = new String[signLength];
        for (int i = 0; i < signLength; ++i) {
            String signId = packet.readUtf(Short.MAX_VALUE);
            signIds[i] = signId.isEmpty() ? null : signId;
        }
        minecraftServer.execute(() -> {
            BlockEntity entity = player.serverLevel().getBlockEntity(signPos);
            if (entity instanceof BlockShanghaiSign.TileEntityRailwaySign) {
                SHMNetUtils.setTileEntityDataAndWriteUpdate(player, entity2 -> entity2.setData(selectedIds, signIds),
                    new BlockShanghaiSign.TileEntityRailwaySign[]{(BlockShanghaiSign.TileEntityRailwaySign) entity});
            } else if (entity instanceof BlockRouteSignBase.TileEntityRouteSignBase) {
                long platformId = selectedIds.isEmpty() ? 0L : (Long) selectedIds.toArray()[0];
                BlockEntity entityAbove = player.serverLevel().getBlockEntity(signPos.above());
                if (entityAbove instanceof BlockRouteSignBase.TileEntityRouteSignBase) {
                    SHMNetUtils.setTileEntityDataAndWriteUpdate(player, entity2 -> entity2.setPlatformId(platformId),
                        new BlockRouteSignBase.TileEntityRouteSignBase[]{(BlockRouteSignBase.TileEntityRouteSignBase) entityAbove, (BlockRouteSignBase.TileEntityRouteSignBase) entity});
                } else {
                    SHMNetUtils.setTileEntityDataAndWriteUpdate(player, entity2 -> entity2.setPlatformId(platformId),
                        new BlockRouteSignBase.TileEntityRouteSignBase[]{(BlockRouteSignBase.TileEntityRouteSignBase) entity});
                }
            }
        });
    }
}
