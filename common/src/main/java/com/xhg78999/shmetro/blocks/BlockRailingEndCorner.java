package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.blocks.BlockRailingDoor;
import com.xhg78999.shmetro.blocks.BlockRailingGlass;
import mtr.block.BlockAPGGlass;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;

public class BlockRailingEndCorner
extends BlockAPGGlass {
    public static final BooleanProperty MIRRORED = BooleanProperty.create((String)"mirrored");

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        boolean mirrored = (Boolean)IBlock.getStatePropertySafe((BlockState)state, (Property)MIRRORED);
        return mirrored ? (IBlock.getStatePropertySafe((BlockState)state, (Property)HALF) == DoubleBlockHalf.UPPER ? Shapes.or((VoxelShape)IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)9.0, (double)3.0, (Direction)facing), (VoxelShape)IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)3.0, (double)9.0, (double)16.0, (Direction)facing)) : Shapes.or((VoxelShape)IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)3.0, (Direction)facing), (VoxelShape)IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)3.0, (double)16.0, (double)16.0, (Direction)facing))) : (IBlock.getStatePropertySafe((BlockState)state, (Property)HALF) == DoubleBlockHalf.UPPER ? Shapes.or((VoxelShape)IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)9.0, (double)3.0, (Direction)facing), (VoxelShape)IBlock.getVoxelShapeByDirection((double)13.0, (double)0.0, (double)0.0, (double)16.0, (double)9.0, (double)16.0, (Direction)facing)) : Shapes.or((VoxelShape)IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)3.0, (Direction)facing), (VoxelShape)IBlock.getVoxelShapeByDirection((double)13.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)16.0, (Direction)facing)));
    }

    public BlockState updateShape(BlockState state, Direction direction, BlockState newState, LevelAccessor world, BlockPos pos, BlockPos posFrom) {
        boolean leftValid;
        boolean isTop = IBlock.getStatePropertySafe((BlockState)state, (Property)HALF) == DoubleBlockHalf.UPPER;
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        BlockState leftState = world.getBlockState(pos.relative(facing.getOpposite()));
        boolean bl = leftValid = leftState.getBlock() instanceof BlockRailingDoor || leftState.getBlock() instanceof BlockRailingGlass;
        return (isTop && direction == Direction.DOWN || !isTop && direction == Direction.UP) && !newState.is((Block)this) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : (leftValid ? (BlockState)state.setValue((Property)MIRRORED, (Comparable)Boolean.valueOf(false)) : (BlockState)state.setValue((Property)MIRRORED, (Comparable)Boolean.valueOf(true)));
    }

    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (IBlock.getStatePropertySafe((BlockState)state, (Property)HALF) == DoubleBlockHalf.UPPER) {
            IBlock.onBreakCreative((Level)world, (Player)player, (BlockPos)pos.below());
        }
        super.playerWillDestroy(world, pos, state, player);
    }

    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        return InteractionResult.SUCCESS;
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection();
        if (facing == null || facing.getAxis() == Direction.Axis.Y) {
            facing = Direction.NORTH;
        }
        return IBlock.isReplaceable((BlockPlaceContext)ctx, (Direction)Direction.UP, (int)2) ? (BlockState)((BlockState)this.getAdditionalState(ctx.getClickedPos(), facing).setValue((Property)FACING, (Comparable)facing)).setValue((Property)HALF, (Comparable)DoubleBlockHalf.LOWER) : null;
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)rotation.rotate((Direction)state.getValue((Property)FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)mirror.mirror((Direction)state.getValue((Property)FACING)));
    }

    public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, HALF, MIRRORED});
    }
}

