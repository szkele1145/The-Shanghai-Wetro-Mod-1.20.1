package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.blocks.BlockShanghaiSign;
import mtr.block.BlockPoleCheckBase;
import mtr.mappings.Text;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockShanghaiSignPole
extends BlockPoleCheckBase {
    public BlockShanghaiSignPole(BlockBehaviour.Properties settings) {
        super(settings);
    }

    protected boolean isBlock(Block block) {
        return block instanceof BlockShanghaiSign && ((BlockShanghaiSign)block).length > 0 || block instanceof BlockShanghaiSignPole;
    }

    protected Component getTooltipBlockText() {
        return Text.translatable((String)"block.mtr.railway_sign", (Object[])new Object[0]);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }
}

