package com.xhg78999.shmetro.network;

import com.xhg78999.shmetro.entity.BlockEntityGlobalPosition;
import com.xhg78999.shmetro.network.SHMPacketDefine;
import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import mtr.Registry;
import mtr.mappings.BlockEntityMapper;
import mtr.packet.PacketTrainDataBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

public class SHMPacketGDServer
extends PacketTrainDataBase {
    public static void openSHMGDScreenS2C(ServerPlayer player, BlockPos signPos) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(signPos);
        Registry.sendToPlayer((ServerPlayer)player, (ResourceLocation)SHMPacketDefine.PACKET_SHM_OPEN_GD_SCREEN, (FriendlyByteBuf)packet);
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

    public static void receiveSHMGDUpdateC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        BlockPos gdPos = packet.readBlockPos();
        String gdId = packet.readUtf();
        minecraftServer.execute(() -> {
            BlockEntity entity = player.serverLevel().getBlockEntity(gdPos);
            if (entity instanceof BlockEntityGlobalPosition) {
                SHMPacketGDServer.setTileEntityDataAndWriteUpdate(player, entity2 -> {
                    entity2.code = gdId;
                    entity2.syncData();
                }, new BlockEntityGlobalPosition[]{(BlockEntityGlobalPosition) entity});
            }
        });
    }
}

