package com.xhg78999.shmetro.items;

import com.xhg78999.shmetro.Blocks;
import com.xhg78999.shmetro.CreativeTab;
import com.xhg78999.shmetro.blocks.BlockShanghaiPSDTop;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

public class ItemShanghaiPSD
extends ItemWithCreativeTabBase
implements IBlock {
    private final int type;

    public ItemShanghaiPSD(int type) {
        super(CreativeTab.SHFACILITIES);
        this.type = type;
    }

    public InteractionResult use(UseOnContext context) {
        int horizontalBlocks;
        int n = horizontalBlocks = this.type >= 20 ? 2 : 1;
        if (ItemShanghaiPSD.blocksNotReplaceable(context, horizontalBlocks, 3, this.getBlockStateFromItem().getBlock())) {
            return InteractionResult.FAIL;
        }
        Level world = context.getLevel();
        Direction playerFacing = context.getClickedFace();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        for (int x = 0; x < horizontalBlocks; ++x) {
            BlockPos newPos = pos.relative(playerFacing.getClockWise(), x);
            for (int y = 0; y < 2; ++y) {
                BlockState state = (BlockState)((BlockState)this.getBlockStateFromItem().setValue((Property)BlockPSDAPGBase.FACING, (Comparable)playerFacing)).setValue((Property)HALF, (Comparable)(y == 1 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER));
                if (this.type >= 20) {
                    BlockState newState = (BlockState)state.setValue((Property)SIDE, (Comparable)(x == 0 ? IBlock.EnumSide.LEFT : IBlock.EnumSide.RIGHT));
                    world.setBlockAndUpdate(newPos.above(y), newState);
                    continue;
                }
                world.setBlockAndUpdate(newPos.above(y), (BlockState)state.setValue((Property)SIDE_EXTENDED, (Comparable)IBlock.EnumSide.SINGLE));
            }
            BlockPos checkPos = newPos.above(2);
            BlockState checkState = BlockShanghaiPSDTop.getActualState((BlockGetter)world, newPos.above(2));
            world.setBlockAndUpdate(checkPos, (BlockState)checkState.setValue(BlockShanghaiPSDTop.KIND, switch (this.type) {
                case 1 -> BlockShanghaiPSDTop.EnumPSDTop.RAILMAP;
                case 21 -> BlockShanghaiPSDTop.EnumPSDTop.RAILMAP;
                case 0 -> BlockShanghaiPSDTop.EnumPSDTop.DESTINATION;
                case 20 -> BlockShanghaiPSDTop.EnumPSDTop.RUNIN;
                default -> BlockShanghaiPSDTop.EnumPSDTop.BLANK;
            }));
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    public void appendHoverText(ItemStack itemStack, Level level, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add((Component)Text.translatable((String)("tooltip.shmetro." + (this.type == 1 || this.type == 21 ? "railmap" : (this.type == 0 ? "destination" : (this.type == 20 ? "runin" : "blank")))), (Object[])new Object[0]).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
    }

    private BlockState getBlockStateFromItem() {
        return switch (this.type) {
            case 0 -> ((Block)Blocks.SH_PSD_GLASS_DESTINATION.get()).defaultBlockState();
            case 1 -> ((Block)Blocks.SH_PSD_GLASS_RAILMAP.get()).defaultBlockState();
            case 10 -> ((Block)Blocks.SH_PSD_END.get()).defaultBlockState();
            case 19 -> ((Block)Blocks.SH_PSD_GLASS_BLANK.get()).defaultBlockState();
            case 20 -> ((Block)Blocks.SH_PSD_DOOR_RUNIN.get()).defaultBlockState();
            case 21 -> ((Block)Blocks.SH_PSD_DOOR_RAILMAP.get()).defaultBlockState();
            case 22 -> ((Block)Blocks.SH_PSD_DOOR_BLANK.get()).defaultBlockState();
            default -> net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        };
    }

    public static boolean blocksNotReplaceable(UseOnContext context, int width, int height, Block blacklistBlock) {
        Direction facing = context.getClickedFace();
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

