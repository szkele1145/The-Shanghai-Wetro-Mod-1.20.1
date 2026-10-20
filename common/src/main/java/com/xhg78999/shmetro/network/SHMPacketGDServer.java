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
import net.minecraft.world.phys.Vec3;

public class SHMPacketGDServer
extends PacketTrainDataBase {
    /**
     * GD 编号的最大长度。
     *
     * <p>实际取值形如 {@code "GD 000000"}, 8 个字符足够。</p>
     *
     * <p>原实现用 {@code packet.readUtf()}(无参)读取, 长度上限是
     * {@code FriendlyByteBuf} 的 32767 字符。服务端拿到的是客户端完全可控的
     * 字符串, 而 {@code code} 会被:</p>
     * <ul>
     *   <li>{@code syncData()} 广播给附近所有客户端;</li>
     *   <li>{@code RenderGlobalPosition} 用 {@code drawTextFont} 逐字符绘制,
     *       32767 字符意味着每帧生成 32767 个文本四边形 -&gt; 客户端渲染卡死。</li>
     * </ul>
     * <p>因此在协议层就限长: 超长的包无法通过 {@code readUtf(maxLength)}
     * 解析, 会被 MC 的网络层当作非法包断开连接。</p>
     */
    private static final int MAX_GD_CODE_LENGTH = SHMPacketDefine.MAX_GD_CODE_LENGTH;

    public static void openSHMGDScreenS2C(ServerPlayer player, BlockPos signPos) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(signPos);
        Registry.sendToPlayer((ServerPlayer)player, (ResourceLocation)SHMPacketDefine.PACKET_SHM_OPEN_GD_SCREEN, (FriendlyByteBuf)packet);
    }

    public static void receiveSHMGDUpdateC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        BlockPos gdPos = packet.readBlockPos();
        // 限长读取: 超过 MAX_GD_CODE_LENGTH 的字符串会抛 DecoderException,
        // 由 MC 网络层断开该连接, 避免超长文本被广播 + 逐字渲染。
        String gdId = packet.readUtf(MAX_GD_CODE_LENGTH);
        // 二次校验: 控制字符(换行/方向键等)会让 3D 文本渲染错乱,
        // 也可能被用来在标识牌上画不可见字符。直接丢弃该包, 保持原值不变。
        if (containsControlCharacter(gdId)) {
            com.xhg78999.shmetro.Main.LOGGER.warn("SHM: 丢弃含控制字符的 GD 编号(player=" + player.getName().getString() + ")");
            return;
        }
        // 距离校验: gdPos 完全由客户端提供, 原实现照单全收 ⇒ 恶意客户端
        // 可以改写世界任意位置的 GD 编号。正常玩法下这个界面是右键方块
        // 打开的, 玩家就在方块旁边, 所以这个限制不会影响正常使用。
        //
        // 放在 execute 里面做: 玩家坐标由主线程更新, 在网络线程读它
        // 不是线程安全的(读到的可能是略旧的位置, 极端情况下会误判)。
        minecraftServer.execute(() -> {
            if (!isNear(player, gdPos)) {
                com.xhg78999.shmetro.Main.LOGGER.warn("SHM: 丢弃距离过远的 GD 编号包(player=" + player.getName().getString() + ", pos=" + gdPos + ")");
                return;
            }
            BlockEntity entity = player.serverLevel().getBlockEntity(gdPos);
            if (entity instanceof BlockEntityGlobalPosition) {
                SHMNetUtils.setTileEntityDataAndWriteUpdate(player, entity2 -> {
                    entity2.code = gdId;
                    entity2.syncData();
                }, new BlockEntityGlobalPosition[]{(BlockEntityGlobalPosition) entity});
            }
        });
    }

    /**
     * 判断玩家是否离目标方块足够近, 可以修改它的数据。
     *
     * <p>用方块中心点的距离算, 阈值见
     * {@link SHMPacketDefine#MAX_EDIT_DISTANCE_SQUARED}。</p>
     */
    public static boolean isNear(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= SHMPacketDefine.MAX_EDIT_DISTANCE_SQUARED;
    }

    /** 判断字符串是否含有 ASCII 控制字符(C0 区与 DEL)。空格(0x20)及以上视为正常字符。 */
    private static boolean containsControlCharacter(String text) {
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return true;
            }
        }
        return false;
    }
}