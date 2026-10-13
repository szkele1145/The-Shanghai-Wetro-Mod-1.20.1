package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.Items;
import mtr.block.BlockPSDAPGGlassEndBase;
import mtr.block.BlockPSDGlassEnd;
import mtr.block.IBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class BlockShanghaiPSDEnd
extends BlockPSDGlassEnd {
    public BlockShanghaiPSDEnd() {
        super(114514);
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

    private void connectGlass(Level world, BlockPos pos, BlockState state) {
        BlockPos rightPos;
        BlockState rightState;
        boolean rightValid;
        if (IBlock.getStatePropertySafe((BlockState)state, (Property)TOUCHING_LEFT) != BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide.NONE || IBlock.getStatePropertySafe((BlockState)state, (Property)TOUCHING_RIGHT) != BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide.NONE) {
            return;
        }
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
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

