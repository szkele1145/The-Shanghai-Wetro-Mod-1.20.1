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
        Direction facing = ctx.getHorizontalDirection();
        if (facing == null || facing.getAxis() == Direction.Axis.Y) {
            facing = Direction.NORTH;
        }
        return (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)facing);
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

