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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockBehaviour;
import com.xhg78999.shmetro.utils.WallPlacementUtil;

public class BlockEmergencyExit
extends BlockDirectionalMapper
implements IBlock {
    public static final IntegerProperty LOCATION = IntegerProperty.create((String)"type", (int)0, (int)2);

    public BlockEmergencyExit(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 【1.1.49 修朝向】上游 1.19.4 原件用的是 `ctx.getClickedFace()` —— 语义正确
        // (点哪面墙, 出口灯的正面就朝哪边; 见 models/block/emergency_exit_left.json:
        //  薄板 x 跨度仅 0.4 而 z 跨度 6.4, 即**垂直于 X 轴**, 而
        //  blockstates/emergency_exit.json 里 facing=west 不带 y 旋转 ——
        //  默认模型正面朝西, 与 facing=west 精确吻合)。
        //
        // 移植时换成了 `ctx.getHorizontalDirection()`(玩家面朝的方向) —— 贴墙朝向整个错位:
        // 玩家面朝北、点西墙, 结果出口灯装到北墙去。
        // 同一批 1.1.36/1.1.37 已经把 BlockEmergencyStop 等 9 个类改用
        // WallPlacementUtil.getWallFacing, 这里是**漏网的一个**。
        //
        // 另外原代码的 `facing == null || facing.getAxis() == Axis.Y` 是死代码:
        // BlockPlaceContext.getHorizontalDirection() 只会返回 4 个水平方向,
        // 既不会 null 也不会是 Y 轴。而直接用 getClickedFace() 又会在点顶/底面时
        // 拿到 UP/DOWN —— FACING 属性只接受水平四方向, setValue 会抛
        // IllegalArgumentException(上游缺陷)。getWallFacing 两者都兼顾:
        // 优先取水平点击面, 点顶/底面则回退为玩家水平朝向。
        Direction facing = WallPlacementUtil.getWallFacing(ctx);
        return (BlockState)((BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)facing)).setValue((Property)LOCATION, (Comparable)Integer.valueOf(0));
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)rotation.rotate((Direction)state.getValue((Property)FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)mirror.mirror((Direction)state.getValue((Property)FACING)));
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public VoxelShape getOcclusionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return Shapes.empty();
    }

    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        return IBlock.getVoxelShapeByDirection((double)1.0, (double)2.0, (double)0.0, (double)15.0, (double)14.0, (double)3.0, (Direction)facing);
    }

    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        return IBlock.checkHoldingBrush((Level)world, (Player)player, () -> world.setBlockAndUpdate(pos, (BlockState)state.cycle((Property)LOCATION)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, LOCATION});
    }
}

