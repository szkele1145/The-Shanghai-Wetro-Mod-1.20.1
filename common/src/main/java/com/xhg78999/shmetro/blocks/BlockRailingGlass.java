package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.Items;
import mtr.block.BlockAPGGlass;
import mtr.block.IBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;

public class BlockRailingGlass
extends BlockAPGGlass {
    public static final IntegerProperty ARROW_DIRECTION = IntegerProperty.create((String)"propagate_property", (int)0, (int)3);
    public final int type;

    public BlockRailingGlass(int type) {
        this.type = type;
    }

    public Item asItem() {
        return this.type == 1 ? (Item)Items.ERAILING_GLASS_1.get() : (Item)Items.ERAILING_GLASS_2.get();
    }

    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        double y = blockHitResult.getLocation().y;
        return IBlock.getStatePropertySafe((BlockState)state, (Property)HALF) == DoubleBlockHalf.UPPER && y - Math.floor(y) > 0.21875 ? IBlock.checkHoldingBrush((Level)world, (Player)player, () -> {
            world.setBlockAndUpdate(pos, (BlockState)state.cycle((Property)ARROW_DIRECTION));
            this.propagate(world, pos, ((Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING)).getClockWise(), (Property)ARROW_DIRECTION, 3);
            this.propagate(world, pos, ((Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING)).getOpposite(), (Property)ARROW_DIRECTION, 3);
        }) : super.use(state, world, pos, player, interactionHand, blockHitResult);
    }

    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
        int height = IBlock.getStatePropertySafe((BlockState)state, (Property)HALF) == DoubleBlockHalf.UPPER ? 9 : 16;
        return IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)height, (double)4.0, (Direction)((Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, HALF, SIDE_EXTENDED, ARROW_DIRECTION});
    }
}

