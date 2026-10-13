package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.entity.BlockEntityStationInfoBoardSingle;
import mtr.block.BlockDirectionalDoubleBlockBase;
import mtr.block.IBlock;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockStationInfoBoardSingle
extends BlockDirectionalDoubleBlockBase
implements EntityBlock {
    public BlockStationInfoBoardSingle(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public VoxelShape getOcclusionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return Shapes.empty();
    }

    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        return IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)5.0, (double)16.0, (double)16.0, (double)11.0, (Direction)facing);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, HALF});
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (IBlock.getStatePropertySafe((BlockState)state, (Property)HALF) == DoubleBlockHalf.UPPER) {
            return null;
        }
        return new BlockEntityStationInfoBoardSingle(pos, state);
    }
}

