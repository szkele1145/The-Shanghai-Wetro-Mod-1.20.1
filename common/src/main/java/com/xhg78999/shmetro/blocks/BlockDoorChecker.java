package com.xhg78999.shmetro.blocks;

import mtr.block.IBlock;
import mtr.mappings.BlockDirectionalMapper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockDoorChecker
extends BlockDirectionalMapper
implements IBlock {
    public static final BooleanProperty MIRRORED = BooleanProperty.create((String)"mirrored");

    public BlockDoorChecker(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        boolean mirrored = (Boolean)IBlock.getStatePropertySafe((BlockState)state, (Property)MIRRORED);
        return mirrored ? IBlock.getVoxelShapeByDirection((double)10.0, (double)0.0, (double)0.0, (double)16.0, (double)32.0, (double)6.0, (Direction)facing) : IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)6.0, (double)32.0, (double)6.0, (Direction)facing);
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 门检器是贴墙薄板 (getShape 厚度仅 6.0/16):
        // FACING = 薄板所在的那一面 (models/block/door_checker.json 默认贴北面;
        //  blockstates/door_checker.json facing=north 不旋转)。
        // 因此取玩家点击的那一面 —— 点哪面墙就装哪面墙。
        // 点在顶面/底面时回退为玩家水平朝向 (回退而非 null: 该方块也要能落地放置)。
        Direction facing = ctx.getClickedFace();
        if (facing == null || facing.getAxis() == Direction.Axis.Y) {
            facing = ctx.getHorizontalDirection();
        }
        if (facing == null || facing.getAxis() == Direction.Axis.Y) {
            facing = Direction.NORTH;
        }
        return (BlockState)((BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)facing)).setValue((Property)MIRRORED, (Comparable)Boolean.valueOf(false));
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)rotation.rotate((Direction)state.getValue((Property)FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)mirror.mirror((Direction)state.getValue((Property)FACING)));
    }

    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        return IBlock.checkHoldingBrush((Level)world, (Player)player, () -> world.setBlockAndUpdate(pos, (BlockState)state.cycle((Property)MIRRORED)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, MIRRORED});
    }
}

