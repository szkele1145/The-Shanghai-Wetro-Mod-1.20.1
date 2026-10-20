package com.xhg78999.shmetro.blocks;

import mtr.block.IBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import com.xhg78999.shmetro.utils.WallPlacementUtil;

public class BlockBeliefBeaconOnWall
extends HorizontalDirectionalBlock
implements IBlock {
    public BlockBeliefBeaconOnWall(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        return IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)4.0, (Direction)facing);
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public VoxelShape getOcclusionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return Shapes.empty();
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 本方块是贴墙薄板: FACING 的语义是「薄板所在的那一面」
        // (见 models/block/belief_beacon_onwall.json: 模型默认贴北面,
        //  blockstates/belief_beacon_onwall.json: facing=north 不旋转)。
        // 因此放置时应取玩家点击的那一面 —— 点哪面墙就贴哪面墙。
        //
        // 上游 shmetro 原件直接用 ctx.getClickedFace() 但未做水平化处理:
        // 点在方块顶面/底面时会把 FACING 设成 UP/DOWN, 而 FACING 是
        // HorizontalDirectionalBlock.FACING (只含 4 个水平方向) ->
        // setValue 抛 IllegalArgumentException, 放置直接崩。
        // 移植版曾改成 ctx.getHorizontalDirection() 规避崩溃, 但那是
        // 「玩家朝向」而非「墙面所在面」, 贴墙朝向是错的。
        // 现修正为: 取点击面, 上下面则拒绝放置 (返回 null = 放置失败)。
        Direction facing = WallPlacementUtil.getWallFacingOrNull(ctx);
        if (facing == null) {
            return null;
        }
        return (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)facing);
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)rotation.rotate((Direction)state.getValue((Property)FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)mirror.mirror((Direction)state.getValue((Property)FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }
}

