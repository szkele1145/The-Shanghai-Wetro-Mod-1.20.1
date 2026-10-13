package com.xhg78999.shmetro.entity;

import com.xhg78999.shmetro.BlockEntity;
import mtr.mappings.BlockEntityClientSerializableMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityPassing
extends BlockEntityClientSerializableMapper {
    private final boolean entrance;

    public BlockEntityPassing(BlockPos pos, BlockState state, int type, boolean entrance) {
        super(BlockEntityPassing.predictType(type, entrance), pos, state);
        this.entrance = entrance;
    }

    private static BlockEntityType<?> predictType(int type, boolean entrance) {
        return switch (type) {
            case 0 -> {
                if (entrance) {
                    yield (BlockEntityType)BlockEntity.PASSING_2000_IN_TILE_ENTITY.get();
                }
                yield (BlockEntityType)BlockEntity.PASSING_2000_OUT_TILE_ENTITY.get();
            }
            case 1 -> {
                if (entrance) {
                    yield (BlockEntityType)BlockEntity.PASSING_2008_IN_TILE_ENTITY.get();
                }
                yield (BlockEntityType)BlockEntity.PASSING_2008_OUT_TILE_ENTITY.get();
            }
            case 2 -> {
                if (entrance) {
                    yield (BlockEntityType)BlockEntity.PASSING_2010_IN_TILE_ENTITY.get();
                }
                yield (BlockEntityType)BlockEntity.PASSING_2010_OUT_TILE_ENTITY.get();
            }
            case 3 -> {
                if (entrance) {
                    yield (BlockEntityType)BlockEntity.PASSING_2020_IN_TILE_ENTITY.get();
                }
                yield (BlockEntityType)BlockEntity.PASSING_2020_OUT_TILE_ENTITY.get();
            }
            default -> null;
        };
    }

    public boolean isEntrance() {
        return this.entrance;
    }
}

