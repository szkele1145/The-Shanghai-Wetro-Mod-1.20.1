package com.xhg78999.shmetro.blocks;

import mtr.block.IBlock;
import mtr.mappings.BlockDirectionalMapper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;

/**
 * 上海地铁“禁止携带物品”公告牌。
 * 双面薄牌（1 像素厚），可贴墙或落地，四个水平朝向。
 * 1.0.5 新增。
 */
public class BlockProhibitedList extends BlockDirectionalMapper {

    public BlockProhibitedList() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .sound(SoundType.METAL)
                .strength(0.5F)
                .noOcclusion()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
                .lightLevel(state -> 12));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 禁带物品告示牌是贴墙薄板: FACING = 薄板所在的那一面
        // (models/block/prohibited_list.json 默认贴北面;
        //  blockstates/prohibited_list.json facing=north 不旋转)。
        // 取玩家点击的那一面 —— 点哪面墙就挂哪面墙; 上下面拒绝放置。
        // 上游原件用 ctx.getClickedFace() 但未做水平化, 点顶/底面会
        // 把 FACING 设成 UP/DOWN -> setValue 抛 IllegalArgumentException。
        Direction facing = ctx.getClickedFace();
        if (facing == null || facing.getAxis() == Direction.Axis.Y) {
            return null;
        }
        return this.defaultBlockState().setValue((Property) FACING, (Comparable) facing);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }


    @Override
    public net.minecraft.world.InteractionResult use(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        // 手持 MTR 刷子右键: 旋转告示牌朝向
        return mtr.block.IBlock.checkHoldingBrush(level, player, () -> {
            net.minecraft.core.Direction facing = (net.minecraft.core.Direction) mtr.block.IBlock.getStatePropertySafe(state, FACING);
            level.setBlock(pos, state.setValue((net.minecraft.world.level.block.state.properties.Property) FACING, (java.lang.Comparable) facing.getClockWise()), 3);
        });
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
        return IBlock.getVoxelShapeByDirection(1.0, 1.0, 7.5, 15.0, 15.0, 8.5, (Direction) IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}
