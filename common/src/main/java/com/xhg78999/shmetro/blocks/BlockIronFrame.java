package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.Blocks;
import com.xhg78999.shmetro.entity.BlockEntityIronFrame;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockIronFrame
extends FenceBlock
implements EntityBlock {
    public BlockIronFrame(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{NORTH, EAST, WEST, SOUTH, WATERLOGGED});
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level blockGetter = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        BlockPos blockPos2 = blockPos.north();
        BlockPos blockPos3 = blockPos.east();
        BlockPos blockPos4 = blockPos.south();
        BlockPos blockPos5 = blockPos.west();
        BlockState blockState = blockGetter.getBlockState(blockPos2);
        BlockState blockState2 = blockGetter.getBlockState(blockPos3);
        BlockState blockState3 = blockGetter.getBlockState(blockPos4);
        BlockState blockState4 = blockGetter.getBlockState(blockPos5);
        return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)super.getStateForPlacement(context).setValue((Property)NORTH, (Comparable)Boolean.valueOf(this.connectsTo(blockState, blockState.isFaceSturdy((BlockGetter)blockGetter, blockPos2, Direction.SOUTH), Direction.SOUTH)))).setValue((Property)EAST, (Comparable)Boolean.valueOf(this.connectsTo(blockState2, blockState2.isFaceSturdy((BlockGetter)blockGetter, blockPos3, Direction.WEST), Direction.WEST)))).setValue((Property)SOUTH, (Comparable)Boolean.valueOf(this.connectsTo(blockState3, blockState3.isFaceSturdy((BlockGetter)blockGetter, blockPos4, Direction.NORTH), Direction.NORTH)))).setValue((Property)WEST, (Comparable)Boolean.valueOf(this.connectsTo(blockState4, blockState4.isFaceSturdy((BlockGetter)blockGetter, blockPos5, Direction.EAST), Direction.EAST)))).setValue((Property)WATERLOGGED, (Comparable)Boolean.valueOf(fluidState.getType() == Fluids.WATER));
    }

    public boolean connectsTo(BlockState state, boolean isSideSolid, Direction direction) {
        Block block = state.getBlock();
        return block == Blocks.IRON_FRAME.get();
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityIronFrame(pos, state);
    }
}

