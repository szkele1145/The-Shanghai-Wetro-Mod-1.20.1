package com.xhg78999.shmetro.entity;

import com.xhg78999.shmetro.BlockEntity;
import mtr.mappings.BlockEntityClientSerializableMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityStationInfoBoardSingle
extends BlockEntityClientSerializableMapper {
    public BlockEntityStationInfoBoardSingle(BlockPos pos, BlockState state) {
        super((BlockEntityType)BlockEntity.STATION_INFO_BOARD_SINGLE_TILE_ENTITY.get(), pos, state);
    }
}

