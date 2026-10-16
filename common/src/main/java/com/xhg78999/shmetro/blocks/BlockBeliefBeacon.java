package com.xhg78999.shmetro.blocks;

import net.minecraft.world.level.block.SoundType;
import com.xhg78999.shmetro.entity.BlockEntityBeliefBeacon;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockBeliefBeacon
extends HorizontalDirectionalBlock
implements EntityBlock {
    public BlockBeliefBeacon() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).destroyTime(0.3f).noOcclusion().isValidSpawn((a, b, c, d) -> false).isRedstoneConductor((a, b, c) -> false).isViewBlocking((a, b, c) -> false).hasPostProcess((a, b, c) -> false).lightLevel(blockStatex -> 13));
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

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityBeliefBeacon(pos, state);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }
}

