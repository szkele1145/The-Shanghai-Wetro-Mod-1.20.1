package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.BlockEntity;
import com.xhg78999.shmetro.Items;
import mtr.block.BlockAPGDoor;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;

public class BlockRailingDoor
extends BlockAPGDoor {
    public final int type;

    public BlockRailingDoor(int type) {
        this.type = type;
    }

    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
        int height = IBlock.getStatePropertySafe((BlockState)state, (Property)HALF) == DoubleBlockHalf.UPPER ? 9 : 16;
        return IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)height, (double)4.0, (Direction)((Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING)));
    }

    public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityERailingDoor(pos, state, this.type);
    }

    public Item asItem() {
        return this.type == 1 ? (Item)Items.ERAILING_DOOR_1.get() : (Item)Items.ERAILING_DOOR_2.get();
    }

    public static class TileEntityERailingDoor
    extends BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase {
        public TileEntityERailingDoor(BlockPos pos, BlockState state, int type) {
            super(type == 1 ? (BlockEntityType)BlockEntity.ERAILING_DOOR_TILE_ENTITY_1.get() : (BlockEntityType)BlockEntity.ERAILING_DOOR_TILE_ENTITY_2.get(), pos, state);
        }
    }
}

