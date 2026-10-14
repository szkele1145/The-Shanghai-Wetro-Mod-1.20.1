package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.blocks.BlockSimpleStair;
import mtr.block.IBlock;
import mtr.mappings.BlockDirectionalMapper;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

public class BlockFence
extends BlockDirectionalMapper
implements IBlock {
    public static final EnumProperty<EnumShanghaiFence> TYPE = EnumProperty.create((String)"type", EnumShanghaiFence.class);

    public BlockFence(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, TYPE});
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection();
        BlockState s = (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)facing);
        Level world = ctx.getLevel();
        BlockState downState = world.getBlockState(ctx.getClickedPos().below());
        Block left = world.getBlockState(ctx.getClickedPos().relative(facing.getCounterClockWise())).getBlock();
        Block right = world.getBlockState(ctx.getClickedPos().relative(facing.getClockWise())).getBlock();
        if (downState.getBlock() instanceof StairBlock && downState.hasProperty((Property)StairBlock.FACING) && downState.hasProperty((Property)StairBlock.HALF) && IBlock.getStatePropertySafe((BlockState)downState, (Property)StairBlock.HALF) == Half.BOTTOM) {
            Direction belowFacing = (Direction)IBlock.getStatePropertySafe((BlockState)downState, (Property)BlockSimpleStair.FACING);
            s = left instanceof StairBlock || left instanceof BlockSimpleStair ? (right instanceof StairBlock || right instanceof BlockSimpleStair ? (BlockState)s.setValue(TYPE, EnumShanghaiFence.ORDINARY) : (BlockState)s.setValue(TYPE, EnumShanghaiFence.SLOPE)) : (right instanceof StairBlock || right instanceof BlockSimpleStair ? (BlockState)s.setValue(TYPE, EnumShanghaiFence.SLOPE_MIRRORED) : (belowFacing == facing.getClockWise() ? (BlockState)s.setValue(TYPE, EnumShanghaiFence.SLOPE_TRANSITION_DOWN_MIRRORED) : (belowFacing == facing.getCounterClockWise() ? (BlockState)s.setValue(TYPE, EnumShanghaiFence.SLOPE_TRANSITION_DOWN) : (BlockState)s.setValue(TYPE, EnumShanghaiFence.ORDINARY))));
        } else if (downState.getBlock() instanceof BlockSimpleStair) {
            Direction belowFacing = (Direction)IBlock.getStatePropertySafe((BlockState)downState, (Property)BlockSimpleStair.FACING);
            s = left instanceof StairBlock || left instanceof BlockSimpleStair ? (right instanceof StairBlock || right instanceof BlockSimpleStair ? (BlockState)s.setValue(TYPE, EnumShanghaiFence.ORDINARY) : (BlockState)s.setValue(TYPE, EnumShanghaiFence.SLOPE)) : (right instanceof StairBlock || right instanceof BlockSimpleStair ? (BlockState)s.setValue(TYPE, EnumShanghaiFence.SLOPE_MIRRORED) : (belowFacing == facing.getClockWise() ? (BlockState)s.setValue(TYPE, EnumShanghaiFence.SLOPE_TRANSITION_DOWN_MIRRORED) : (belowFacing == facing.getCounterClockWise() ? (BlockState)s.setValue(TYPE, EnumShanghaiFence.SLOPE_TRANSITION_DOWN) : (BlockState)s.setValue(TYPE, EnumShanghaiFence.ORDINARY))));
        } else {
            s = left instanceof StairBlock || left instanceof BlockSimpleStair ? (BlockState)s.setValue(TYPE, EnumShanghaiFence.SLOPE_TRANSITION_UP) : (right instanceof StairBlock || right instanceof BlockSimpleStair ? (BlockState)s.setValue(TYPE, EnumShanghaiFence.SLOPE_TRANSITION_UP_MIRRORED) : (BlockState)s.setValue(TYPE, EnumShanghaiFence.ORDINARY));
        }
        return s;
    }

    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        EnumShanghaiFence type = (EnumShanghaiFence)((Object)IBlock.getStatePropertySafe((BlockState)state, TYPE));
        return switch (type) {
            case ORDINARY -> IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)19.0, (double)3.0, (Direction)facing);
            default -> IBlock.getVoxelShapeByDirection((double)0.0, (double)-8.0, (double)0.0, (double)16.0, (double)19.0, (double)3.0, (Direction)facing);
        };
    }

    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    public static enum EnumShanghaiFence implements StringRepresentable
    {
        ORDINARY("ordinary"),
        SLOPE("slope"),
        SLOPE_MIRRORED("slope_mirrored"),
        SLOPE_TRANSITION_DOWN("slope_transition_down"),
        SLOPE_TRANSITION_UP("slope_transition_up"),
        SLOPE_TRANSITION_DOWN_MIRRORED("slope_transition_down_mirrored"),
        SLOPE_TRANSITION_UP_MIRRORED("slope_transition_up_mirrored");

        private final String name;

        private EnumShanghaiFence(String nameIn) {
            this.name = nameIn;
        }

        @NotNull
        public String getSerializedName() {
            return this.name;
        }
    }
}

