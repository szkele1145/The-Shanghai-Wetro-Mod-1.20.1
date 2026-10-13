package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.blocks.BlockFence;
import mtr.block.IBlock;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockFenceDouble
extends BlockFence {
    public BlockFenceDouble(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
        return IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)35.0, (double)3.0, (Direction)((Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING)));
    }
}

