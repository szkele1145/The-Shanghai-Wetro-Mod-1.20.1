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
    /** 单个指示牌的最大格数(BlockShanghaiSign.length 的合法上界)。 */
    private static final int MAX_SIGN_LENGTH = 13;
    /** 选中线路 ID 的合理上限(实际线路数量级远小于此)。 */
    private static final int MAX_SELECTED_IDS = 4096;

    public static void openSHMSignScreenS2C(ServerPlayer player, BlockPos signPos) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(signPos);
        Registry.sendToPlayer((ServerPlayer)player, (ResourceLocation)SHMPacketDefine.PACKET_OPEN_SHM_SIGN_SCREEN, (FriendlyByteBuf)packet);
    }

    public static void receiveSHMSignIdsC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        BlockPos signPos = packet.readBlockPos();
        // 长度字段完全由客户端提供: 原实现直接 new String[signLength],
        // 恶意客户端发送 Integer.MAX_VALUE 会抛 OutOfMemoryError(Error, 无法被
        // catch 捕获)导致服务端崩溃。此处加上限校验, 超限直接丢弃该包。
        int selectedIdsLength = packet.readInt();
        if (selectedIdsLength < 0 || selectedIdsLength > MAX_SELECTED_IDS) {
            com.xhg78999.shmetro.Main.LOGGER.warn("SHM: 丢弃非法指示牌包(selectedIdsLength=" + selectedIdsLength + ", player=" + player.getName().getString() + ")");
            return;
        }
        java.util.HashSet<Long> selectedIds = new java.util.HashSet<Long>();
        for (int i = 0; i < selectedIdsLength; ++i) {
            selectedIds.add(packet.readLong());
        }
        int signLength = packet.readInt();
        if (signLength < 0 || signLength > MAX_SIGN_LENGTH) {
            com.xhg78999.shmetro.Main.LOGGER.warn("SHM: 丢弃非法指示牌包(signLength=" + signLength + ", player=" + player.getName().getString() + ")");
            return;
        }
        String[] signIds = new String[signLength];
        for (int i = 0; i < signLength; ++i) {
            String signId = packet.readUtf(Short.MAX_VALUE);
            signIds[i] = signId.isEmpty() ? null : signId;
        }
        // 距离校验: signPos 完全由客户端提供, 原实现照单全收 ⇒ 恶意客户端
        // 可以改写世界任意位置的指示牌。正常玩法下这个界面是右键指示牌
        // 打开的, 玩家就在牌子旁边(发过来的是牌子的某一端端头), 所以这个
        // 限制不会影响正常使用。详见 SHMPacketDefine 的常量说明。
        //
        // 放在 execute 里面做: 玩家坐标由主线程更新, 在网络线程读它
        // 不是线程安全的。
        minecraftServer.execute(() -> {
            if (!SHMPacketGDServer.isNear(player, signPos)) {
                com.xhg78999.shmetro.Main.LOGGER.warn("SHM: 丢弃距离过远的指示牌包(player=" + player.getName().getString() + ", pos=" + signPos + ")");
                return;
            }
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
