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

public class ItemShanghaiRailingDoor
extends ItemWithCreativeTabBase
implements IBlock {
    public final int type;

    public ItemShanghaiRailingDoor(int type) {
        super(CreativeTab.SHFACILITIES);
        this.type = type;
    }

    public InteractionResult use(UseOnContext context) {
        if (ItemPlacementHelper.blocksNotReplaceable(context, 2, 2, this.getBlockStateFromItem().getBlock())) {
            return InteractionResult.FAIL;
        }
        Level world = context.getLevel();
        Direction playerFacing = context.getHorizontalDirection();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        for (int x = 0; x < 2; ++x) {
            BlockPos newPos = pos.relative(playerFacing.getClockWise(), x);
            for (int y = 0; y < 2; ++y) {
                BlockState state = (BlockState)((BlockState)this.getBlockStateFromItem().setValue((Property)BlockPSDAPGBase.FACING, (Comparable)playerFacing)).setValue((Property)HALF, (Comparable)(y == 1 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER));
                BlockState newState = (BlockState)state.setValue((Property)SIDE, (Comparable)(x == 0 ? IBlock.EnumSide.LEFT : IBlock.EnumSide.RIGHT));
                world.setBlockAndUpdate(newPos.above(y), newState);
            }
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    public void appendHoverText(ItemStack itemStack, Level level, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add((Component)Text.translatable((String)"tooltip.shmetro.erailing", (Object[])new Object[0]).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
    }

    private BlockState getBlockStateFromItem() {
        return (this.type == 1 ? (Block)Blocks.ERAILING_DOOR_1.get() : (Block)Blocks.ERAILING_DOOR_2.get()).defaultBlockState();
    }
}

