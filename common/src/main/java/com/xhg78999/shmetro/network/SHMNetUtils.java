package com.xhg78999.shmetro.network;

import java.util.function.Consumer;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.server.level.ServerPlayer;

/**
 * 网络工具类：抽象服务器端包处理中重复的实体数据写入逻辑，
 * 供 SHMPacketTrainDataGuiServer、SHMPacketGDServer 等共用。
 */
public final class SHMNetUtils {

    private SHMNetUtils() {
    }

    /**
     * 对一批同类型方块实体依次应用数据变更。需要在服务器主线程调用。
     *
     * <p>注意：本方法只负责写入数据，不负责同步。若改动需要下发到客户端，
     * 请像 SHMPacketGDServer 那样在 {@code setData} 内显式调用 {@code syncData()}。
     *
     * @param player   触发变更的玩家，为将来按玩家定向发包预留，当前实现未使用
     * @param setData  对单个实体的数据变更操作
     * @param entities 目标实体，允许为空数组
     */
    @SafeVarargs
    public static <T extends BlockEntityMapper> void setTileEntityDataAndWriteUpdate(ServerPlayer player, Consumer<T> setData, T... entities) {
        for (T entity : entities) {
            setData.accept(entity);
        }
    }
}
