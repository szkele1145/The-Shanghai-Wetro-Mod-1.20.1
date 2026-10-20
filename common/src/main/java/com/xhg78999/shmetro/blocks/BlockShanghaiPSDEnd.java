package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.Items;
import mtr.block.BlockPSDAPGGlassEndBase;
import mtr.block.BlockPSDGlassEnd;
import mtr.block.IBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class BlockShanghaiPSDEnd
extends BlockPSDGlassEnd {
    public BlockShanghaiPSDEnd() {
        // MTR BlockPSDGlassEnd(int style) 的 style 只用于它自己的 asItem()
        // 去选 PSD_GLASS_END_1/2; 本类覆写了 asItem()(见下), 该字段不可达。
        // 上游此处写的是占位魔数 114514, 这里改为语义明确的 0, 行为零变化。
        super(0);
    }

    public Item asItem() {
        return (Item)Items.SH_PSD_END.get();
    }

    @NotNull
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        return IBlock.checkHoldingBrush((Level)world, (Player)player, () -> {
            for (int y = -1; y <= 1; ++y) {
                BlockState scanState = world.getBlockState(pos.above(y));
                if (!state.is(scanState.getBlock())) continue;
                this.connectGlass(world, pos.above(y), scanState);
            }
        });
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 【1.1.50 修缺失的空间检查】
        // MTR 基类 BlockDirectionalDoubleBlockBase.getStateForPlacement 的字节码:
        //     Direction dir = ctx.getHorizontalDirection();
        //     if (IBlock.isReplaceable(ctx, Direction.UP, 2)) {
        //         return getAdditionalState(ctx.getClickedPos(), dir)
        //                      .setValue(FACING, dir).setValue(HALF, DoubleBlockHalf.LOWER);
        //     }
        //     return null;                 // 上方不足 2 格空位 -> 拒绝放置
        //
        // 之前的移植版**覆写**了这个方法却只设了 FACING, 把 isReplaceable 的
        // 空间检查整个丢了 —— 屏蔽门端头是 **2 格高**的方块(门体 + 顶端封头),
        // 于是可以把它放在只有 1 格净空的地方, 半截卡在地板/天花板里。
        //
        // 这里改为委托 super: 基类已经用 getHorizontalDirection() 设 FACING
        // 并做 LOWER/HALF 设定, 与本类需要的行为完全一致, 只需要把空间检查
        // 补回来。getAdditionalState 在整条继承链上无人覆写, 等价于
        // defaultBlockState(), 因此委托 super 不会丢任何附加状态。
        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) {
            // 上方空间不足 -> 拒绝放置(与基类一致: 返回 null 让 MC 判定放置失败)
            return null;
        }
        // FACING 语义与基类一致(玩家水平朝向) —— 端头是**立式**方块(与 PSD 玻璃门
        // 门体对齐), 不是贴墙薄板, 故沿用 getHorizontalDirection()。
        // 上面的 null/Axis.Y 检查同样是死代码: getHorizontalDirection() 只会返回
        // 4 个水平方向, 既不会 null 也不会是 Y 轴。
        return state;
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)rotation.rotate((Direction)state.getValue((Property)FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)mirror.mirror((Direction)state.getValue((Property)FACING)));
    }

    private void connectGlass(Level world, BlockPos pos, BlockState state) {
        BlockPos rightPos;
        BlockState rightState;
        boolean rightValid;
        if (IBlock.getStatePropertySafe((BlockState)state, (Property)TOUCHING_LEFT) != BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide.NONE || IBlock.getStatePropertySafe((BlockState)state, (Property)TOUCHING_RIGHT) != BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide.NONE) {
            return;
        }
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        // 左侧必须是 facing.getOpposite(): 玻璃柱的左右两侧是相对的两个面
        // (facing=NORTH 时左=SOUTH, 右=EAST), 构成对称的一对。
        // 原实现误用 getCounterClockWise() (逆时针 90 度, NORTH->WEST),
        // 会让左右变成相对方向(WEST/EAST), 破坏连接几何。
        BlockPos leftPos = pos.relative(facing.getOpposite());
        BlockState leftState = world.getBlockState(leftPos);
        boolean leftValid = this.canConnect(leftState);
        if (leftValid) {
            IBlock.EnumSide side = (IBlock.EnumSide)IBlock.getStatePropertySafe((BlockState)leftState, (Property)SIDE_EXTENDED);
            IBlock.EnumSide newLeftSide = side == IBlock.EnumSide.RIGHT ? IBlock.EnumSide.MIDDLE : (side == IBlock.EnumSide.SINGLE ? IBlock.EnumSide.LEFT : side);
            world.setBlockAndUpdate(leftPos, (BlockState)leftState.setValue((Property)SIDE_EXTENDED, (Comparable)newLeftSide));
        }
        if (rightValid = this.canConnect(rightState = world.getBlockState(rightPos = pos.relative(facing.getClockWise())))) {
            IBlock.EnumSide side = (IBlock.EnumSide)IBlock.getStatePropertySafe((BlockState)rightState, (Property)SIDE_EXTENDED);
            IBlock.EnumSide newRightSide = side == IBlock.EnumSide.LEFT ? IBlock.EnumSide.MIDDLE : (side == IBlock.EnumSide.SINGLE ? IBlock.EnumSide.RIGHT : side);
            world.setBlockAndUpdate(rightPos, (BlockState)rightState.setValue((Property)SIDE_EXTENDED, (Comparable)newRightSide));
        }
        IBlock.EnumSide newSide = leftValid && rightValid ? IBlock.EnumSide.MIDDLE : (leftValid ? IBlock.EnumSide.RIGHT : (rightValid ? IBlock.EnumSide.LEFT : IBlock.EnumSide.SINGLE));
        world.setBlockAndUpdate(pos, (BlockState)state.setValue((Property)SIDE_EXTENDED, (Comparable)newSide));
    }

    private boolean canConnect(BlockState state) {
        if (state.getBlock() instanceof BlockShanghaiPSDEnd) {
            BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide leftside = (BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide)IBlock.getStatePropertySafe((BlockState)state, (Property)TOUCHING_LEFT);
            BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide rightside = (BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide)IBlock.getStatePropertySafe((BlockState)state, (Property)TOUCHING_RIGHT);
            return leftside == BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide.NONE && rightside == BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide.NONE;
        }
        return false;
    }
}

