package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.entity.BlockEntityPassing;
import mtr.block.BlockTicketBarrier;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockPassingSM
extends BlockTicketBarrier
implements EntityBlockMapper {
    private final int type;
    private final boolean isEntrance;

    public BlockPassingSM(int type, boolean isEntrance) {
        super(isEntrance);
        this.type = type;
        this.isEntrance = isEntrance;
    }

    public int getPassingType() {
        return this.type;
    }

    public boolean getEntrance() {
        return this.isEntrance;
    }

    public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPassing(pos, state, this.type, this.isEntrance);
    }
}

