package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.Blocks;
import java.util.Map;
import mtr.block.IBlock;
import mtr.mappings.BlockDirectionalMapper;
import net.minecraft.Util;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockSimpleStair
extends BlockDirectionalMapper
implements IBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create((String)"left");
    public static final BooleanProperty RIGHT = BooleanProperty.create((String)"right");
    protected static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = (Map)PipeBlock.PROPERTY_BY_DIRECTION.entrySet().stream().filter(entry -> ((Direction)entry.getKey()).getAxis().isHorizontal()).collect(Util.toMap());

    public BlockSimpleStair(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        return Shapes.or((VoxelShape)IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)8.0, (double)16.0, (Direction)facing), (VoxelShape)IBlock.getVoxelShapeByDirection((double)0.0, (double)8.0, (double)0.0, (double)16.0, (double)16.0, (double)8.0, (Direction)facing));
    }

    public VoxelShape getOcclusionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return Shapes.empty();
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getClickedFace();
        Level level = ctx.getLevel();
        BlockPos blockPos = ctx.getClickedPos();
        BlockPos posRight = blockPos.relative(facing.getOpposite());
        BlockPos posLeft = blockPos.relative(facing.getClockWise());
        return (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)facing)).setValue((Property)LEFT, (Comparable)Boolean.valueOf(level.getBlockState(posLeft).getBlock() == Blocks.FLOOR_STAIR.get()))).setValue((Property)RIGHT, (Comparable)Boolean.valueOf(level.getBlockState(posRight).getBlock() == Blocks.FLOOR_STAIR.get()));
    }

    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.cycle(FACING).getValue(FACING);
        BlockPos posRight = pos.relative(facing.getOpposite());
        BlockPos posLeft = pos.relative(facing.getClockWise());
        return (BlockState)((BlockState)((BlockState)state.setValue((Property)FACING, (Comparable)facing)).setValue((Property)LEFT, (Comparable)Boolean.valueOf(world.getBlockState(posLeft).getBlock() == Blocks.FLOOR_STAIR.get()))).setValue((Property)RIGHT, (Comparable)Boolean.valueOf(world.getBlockState(posRight).getBlock() == Blocks.FLOOR_STAIR.get()));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, LEFT, RIGHT});
    }
}

