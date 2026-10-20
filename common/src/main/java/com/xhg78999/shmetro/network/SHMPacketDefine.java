package com.xhg78999.shmetro.network;

import net.minecraft.resources.ResourceLocation;

public interface SHMPacketDefine {
    public static final ResourceLocation PACKET_OPEN_SHM_SIGN_SCREEN = new ResourceLocation("shmetro", "packet_open_shm_sign_screen");
    public static final ResourceLocation PACKET_SHM_SIGN_TYPES = new ResourceLocation("shmetro", "packet_shm_sign_types");
    public static final ResourceLocation PACKET_SHM_GD_UPDATE = new ResourceLocation("shmetro", "packet_shm_gd_update");
    public static final ResourceLocation PACKET_SHM_OPEN_GD_SCREEN = new ResourceLocation("shmetro", "packet_open_shm_gd_screen");
    /**
     * GD 编号的最大长度, 客户端与服务端共用。
     *
     * <p>实际取值形如 {@code "GD 000000"}, 8 个字符足够。服务端在
     * {@code SHMPacketGDServer} 用它做 {@code readUtf} 的上限, 客户端在
     * {@code SHMPacketGDClient} 用它截断待发送的字符串 —— 放在这里避免两端
     * 各写一份常量后悄悄对不上。</p>
     */
    int MAX_GD_CODE_LENGTH = 16;

    /**
     * 允许修改方块实体的最大交互距离(方块)。
     *
     * <p>两个 C2S 包(指示牌 / GD 编号)里的方块坐标完全由客户端提供,
     * 服务端原样照单全收 ⇒ 恶意客户端可以改写<b>世界任意位置</b>的方块实体
     * 数据(上游同款, 一直如此)。</p>
     *
     * <p>正常玩法下这两个界面都是<b>右键方块</b>打开的(MC 服务端的交互距离
     * 上限是 6 格), 玩家必然就在目标方块旁边:</p>
     * <ul>
     *   <li>GD 编号: {@code BlockGlobalDirection.use} 直接把被点的方块坐标发过来;</li>
     *   <li>指示牌: {@code BlockShanghaiSign.use} 沿命中方向找到指示牌的<b>端头</b>
     *       发过来 —— 端头可能是牌子的另一端, 最远 13 格, 加上玩家自己的
     *       交互距离 6 格, 总距离最坏约 19 格。</li>
     * </ul>
     *
     * <p>取 24 格是为了覆盖上面那个最坏情况并留出余量, 同时仍能挡住
     * 「隔着半个世界改写方块」这种滥用。服务端取实体时用的是
     * {@code player.serverLevel()}, 因此<b>跨维度</b>的滥用天然无效。</p>
     */
    double MAX_EDIT_DISTANCE_SQUARED = 576.0;
}

