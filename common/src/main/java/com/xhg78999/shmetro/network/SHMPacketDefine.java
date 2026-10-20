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
}

