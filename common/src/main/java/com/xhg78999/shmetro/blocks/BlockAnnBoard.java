package com.xhg78999.shmetro.blocks;

import mtr.block.IBlock;
import mtr.mappings.BlockDirectionalMapper;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import com.xhg78999.shmetro.utils.WallPlacementUtil;

public class BlockAnnBoard
extends BlockDirectionalMapper
implements IBlock {
    public BlockAnnBoard(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public VoxelShape getOcclusionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return Shapes.empty();
    }

    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        return IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)2.0, (double)16.0, (double)32.0, (double)16.0, (Direction)facing);
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 公告牌是贴墙薄板: FACING 的语义是「薄板所在的那一面」
        // (models/block/annboard.json 默认贴北面; blockstates/annboard.json
        //  facing=north 不旋转、facing=south y=180)。
        // 因此取玩家点击的那一面 —— 点哪面墙就挂哪面墙。
        // 上游原件直接用 ctx.getClickedFace() 但未做水平化: 点顶/底面时会把
        // FACING 设成 UP/DOWN, 而 FACING 只含 4 个水平方向 ->
        // setValue 抛 IllegalArgumentException。这里用 getWallFacingOrNull
        // 补齐水平检查, 上下面拒绝放置 (返回 null = 放置失败)。
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

