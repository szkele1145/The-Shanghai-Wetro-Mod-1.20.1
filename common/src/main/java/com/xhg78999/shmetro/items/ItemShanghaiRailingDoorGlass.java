package com.xhg78999.shmetro.items;

import com.xhg78999.shmetro.Blocks;
import com.xhg78999.shmetro.CreativeTab;
import java.util.List;
import mtr.block.BlockPSDAPGBase;
import mtr.block.IBlock;
import mtr.item.ItemWithCreativeTabBase;
import mtr.mappings.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

public class ItemShanghaiRailingDoorGlass
extends ItemWithCreativeTabBase
implements IBlock {
    public final int type;

    public ItemShanghaiRailingDoorGlass(int type) {
        super(CreativeTab.SHFACILITIES);
        this.type = type;
    }

    public InteractionResult use(UseOnContext context) {
        int horizontalBlocks = 1;
        if (ItemShanghaiRailingDoorGlass.blocksNotReplaceable(context, horizontalBlocks, 2, this.getBlockStateFromItem().getBlock())) {
            return InteractionResult.FAIL;
        }
        Level world = context.getLevel();
        Direction playerFacing = context.getHorizontalDirection();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        for (int x = 0; x < horizontalBlocks; ++x) {
            BlockPos newPos = pos.relative(playerFacing.getClockWise(), x);
            for (int y = 0; y < 2; ++y) {
                BlockState state = (BlockState)((BlockState)this.getBlockStateFromItem().setValue((Property)BlockPSDAPGBase.FACING, (Comparable)playerFacing)).setValue((Property)HALF, (Comparable)(y == 1 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER));
                world.setBlockAndUpdate(newPos.above(y), (BlockState)state.setValue((Property)SIDE_EXTENDED, (Comparable)IBlock.EnumSide.SINGLE));
            }
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    public void appendHoverText(ItemStack itemStack, Level level, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add((Component)Text.translatable((String)"tooltip.shmetro.erailing_glass", (Object[])new Object[0]).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
    }

    private BlockState getBlockStateFromItem() {
        return (this.type == 1 ? (Block)Blocks.ERAILING_GLASS_1.get() : (Block)Blocks.ERAILING_GLASS_2.get()).defaultBlockState();
    }

    public static boolean blocksNotReplaceable(UseOnContext context, int width, int height, Block blacklistBlock) {
        Direction facing = context.getHorizontalDirection();
        Level world = context.getLevel();
        BlockPos startingPos = context.getClickedPos().relative(context.getClickedFace());
        for (int x = 0; x < width; ++x) {
            BlockPos offsetPos = startingPos.relative(facing.getClockWise(), x);
            if (blacklistBlock != null) {
                boolean isBlacklistedBelow = world.getBlockState(offsetPos.below()).is(blacklistBlock);
                boolean isBlacklistedAbove = world.getBlockState(offsetPos.above(height)).is(blacklistBlock);
                if (isBlacklistedBelow || isBlacklistedAbove) {
                    return true;
                }
            }
            for (int y = 0; y < height; ++y) {
                if (world.getBlockState(offsetPos.above(y)).liquid()) continue;
                return true;
            }
        }
        return false;
    }
}

