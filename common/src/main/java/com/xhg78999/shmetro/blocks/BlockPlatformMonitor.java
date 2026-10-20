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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockBehaviour;
import com.xhg78999.shmetro.utils.WallPlacementUtil;

public class BlockPlatformMonitor
extends BlockDirectionalMapper
implements IBlock {
    public static final BooleanProperty MIRRORED = BooleanProperty.create((String)"mirrored");

    public BlockPlatformMonitor(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        return IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)32.0, (double)16.0, (Direction)facing);
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public VoxelShape getOcclusionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return Shapes.empty();
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 【1.1.49 修朝向】站台监视器是**贴墙的 2 格高屏幕**:
        //   models/block/platform_monitor.json 的 z 跨度只有 0.25~3 (极薄板),
        //   blockstates/platform_monitor.json 里 facing=north 带 y=180,
        //   说明模型默认朝南、facing 指**正面朝向** = 贴在哪面墙的外侧。
        //
        // 上游 1.19.4 原件用 `ctx.getClickedFace()` —— 语义正确(点哪面墙就贴哪面)。
        // 移植时换成了 `ctx.getHorizontalDirection()`(玩家面朝的方向) —— 贴墙朝向错位:
        // 玩家面朝北、点西墙, 监视器装到北墙上。
        // 与 BlockEmergencyExit 是同一批漏网的类(1.1.36/1.1.37 已修过其中 9 个)。
        //
        // 直接用 getClickedFace() 在点顶/底面时会拿到 UP/DOWN, 而 FACING 只接受
        // 水平四方向 -> setValue 抛 IllegalArgumentException(上游缺陷);
        // getWallFacing 两者兼顾: 优先水平点击面, 点顶/底面回退玩家水平朝向。
        Direction facing = WallPlacementUtil.getWallFacing(ctx);
        return (BlockState)((BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)facing)).setValue((Property)MIRRORED, (Comparable)Boolean.valueOf(false));
    }

    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        return IBlock.checkHoldingBrush((Level)world, (Player)player, () -> world.setBlockAndUpdate(pos, (BlockState)state.cycle((Property)MIRRORED)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, MIRRORED});
    }
}

