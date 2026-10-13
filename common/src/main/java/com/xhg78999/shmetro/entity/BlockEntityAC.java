package com.xhg78999.shmetro.entity;

import com.xhg78999.shmetro.BlockEntity;
import mtr.mappings.BlockEntityClientSerializableMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityAC
extends BlockEntityClientSerializableMapper {
    public BlockEntityAC(BlockPos pos, BlockState state) {
        super((BlockEntityType)BlockEntity.AC_TILE_ENTITY.get(), pos, state);
    }
}

