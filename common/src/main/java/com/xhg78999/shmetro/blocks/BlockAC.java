package com.xhg78999.shmetro.blocks;

import net.minecraft.world.level.block.SoundType;
import com.xhg78999.shmetro.entity.BlockEntityAC;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockAC
extends HorizontalDirectionalBlock
implements EntityBlock {
    public BlockAC() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).destroyTime(0.3f).instabreak().isValidSpawn((a, b, c, d) -> false).isRedstoneConductor((a, b, c) -> false).isViewBlocking((a, b, c) -> false).hasPostProcess((a, b, c) -> false).lightLevel(blockStatex -> 13));
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection();
        return (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)facing);
    }

    public BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return new BlockEntityAC(p_153215_, p_153216_);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }
}

