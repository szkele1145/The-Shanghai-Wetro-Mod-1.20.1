package com.xhg78999.shmetro.network;

import com.xhg78999.shmetro.blocks.BlockShanghaiSign;
import com.xhg78999.shmetro.network.SHMPacketDefine;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.function.Consumer;
import mtr.Registry;
import mtr.block.BlockRouteSignBase;
import mtr.mappings.BlockEntityMapper;
import mtr.packet.PacketTrainDataBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

public class SHMPacketTrainDataGuiServer
extends PacketTrainDataBase {
    public static void openSHMSignScreenS2C(ServerPlayer player, BlockPos signPos) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(signPos);
        Registry.sendToPlayer((ServerPlayer)player, (ResourceLocation)SHMPacketDefine.PACKET_OPEN_SHM_SIGN_SCREEN, (FriendlyByteBuf)packet);
    }

    @SafeVarargs
    private static <T extends BlockEntityMapper> void setTileEntityDataAndWriteUpdate(ServerPlayer player, Consumer<T> setData, T ... entities) {
        if (entities.length > 0) {
            CompoundTag compoundTagOld = new CompoundTag();
            entities[0].writeCompoundTag(compoundTagOld);
            BlockPos blockPos = null;
            long posLong = 0L;
            for (T entity : entities) {
                setData.accept(entity);
                BlockPos entityPos = entity.getBlockPos();
                if (blockPos != null && entityPos.asLong() <= posLong) continue;
                blockPos = entityPos;
                posLong = entityPos.asLong();
            }
            CompoundTag compoundTagNew = new CompoundTag();
            entities[0].writeCompoundTag(compoundTagNew);
        }
    }

    public static void receiveSHMSignIdsC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        BlockPos signPos = packet.readBlockPos();
        int selectedIdsLength = packet.readInt();
        HashSet<Long> selectedIds = new HashSet<Long>();
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
                SHMPacketTrainDataGuiServer.setTileEntityDataAndWriteUpdate(player, entity2 -> entity2.setData(selectedIds, signIds), new BlockShanghaiSign.TileEntityRailwaySign[]{(BlockShanghaiSign.TileEntityRailwaySign) entity});
            } else if (entity instanceof BlockRouteSignBase.TileEntityRouteSignBase) {
                long platformId = selectedIds.isEmpty() ? 0L : (Long)selectedIds.toArray()[0];
                BlockEntity entityAbove = player.serverLevel().getBlockEntity(signPos.above());
                if (entityAbove instanceof BlockRouteSignBase.TileEntityRouteSignBase) {
                    SHMPacketTrainDataGuiServer.setTileEntityDataAndWriteUpdate(player, entity2 -> entity2.setPlatformId(platformId), new BlockRouteSignBase.TileEntityRouteSignBase[]{(BlockRouteSignBase.TileEntityRouteSignBase) entityAbove, (BlockRouteSignBase.TileEntityRouteSignBase) entity});
                } else {
                    SHMPacketTrainDataGuiServer.setTileEntityDataAndWriteUpdate(player, entity2 -> entity2.setPlatformId(platformId), new BlockRouteSignBase.TileEntityRouteSignBase[]{(BlockRouteSignBase.TileEntityRouteSignBase) entity});
                }
            }
        });
    }
}

