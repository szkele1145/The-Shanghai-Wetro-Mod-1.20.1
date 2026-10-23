package com.xhg78999.shmetro.entity;

import com.xhg78999.shmetro.BlockEntity;
import mtr.mappings.BlockEntityClientSerializableMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityGlobalPosition
extends BlockEntityClientSerializableMapper {
    public String code = "GD 000000";

    public BlockEntityGlobalPosition(BlockPos pos, BlockState state) {
        super((BlockEntityType)BlockEntity.GD_TILE_ENTITY.get(), pos, state);
    }

    public void writeCompoundTag(CompoundTag nbt) {
        nbt.putString("code", this.code);
        super.writeCompoundTag(nbt);
    }

    private static final int MAX_CODE_LENGTH = 16;

    public void readCompoundTag(CompoundTag nbt) {
        super.readCompoundTag(nbt);
        String raw = nbt.getString("code");
        if (raw.length() > MAX_CODE_LENGTH) {
            com.xhg78999.shmetro.Main.LOGGER.warn("SHM: 丢弃超长的 GD 编号 (len=" + raw.length() + ")");
            raw = raw.substring(0, MAX_CODE_LENGTH);
        }
        // 【1.1.86 过滤控制字符】防止 NBT 被篡改导致渲染/聊天异常
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); ++i) {
            char c = raw.charAt(i);
            if (c >= 32 && c != 127) {
                sb.append(c);
            }
        }
        raw = sb.toString();
        this.code = raw.isEmpty() ? "GD 000000" : raw;
    }
}

