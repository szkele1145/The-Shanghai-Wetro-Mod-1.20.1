package com.xhg78999.shmetro.blocks;

import com.xhg78999.shmetro.BlockEntity;
import com.xhg78999.shmetro.Items;
import com.xhg78999.shmetro.blocks.BlockShanghaiPSDTop;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.BlockPSDDoor;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockShanghaiPSDDoor
extends BlockPSDDoor {
    public static final EnumProperty<BlockShanghaiPSDTop.EnumPSDTop> KIND = EnumProperty.create((String)"kind", BlockShanghaiPSDTop.EnumPSDTop.class);
    public final BlockShanghaiPSDTop.EnumPSDTop top;

    public BlockShanghaiPSDDoor(BlockShanghaiPSDTop.EnumPSDTop top) {
        super(114514);
        this.top = top;
    }

    public Item asItem() {
        return switch (this.top) {
            case RAILMAP -> (Item)Items.SH_PSD_DOOR_RAILMAP.get();
            case RUNIN -> (Item)Items.SH_PSD_DOOR_RUNIN.get();
            case BLANK -> (Item)Items.SH_PSD_DOOR_BLANK.get();
            default -> null;
        };
    }

    public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySHPSDDoor(this.top, pos, state);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{END, FACING, HALF, SIDE, UNLOCKED, KIND});
    }

    public static class TileEntitySHPSDDoor
    extends BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase {
        private int open;
        private float openClient;

        public TileEntitySHPSDDoor(BlockShanghaiPSDTop.EnumPSDTop top, BlockPos pos, BlockState state) {
            super(switch (top) {
                case RAILMAP -> (BlockEntityType)BlockEntity.SH_PSD_DOOR_RAILMAP_TILE_ENTITY.get();
                case RUNIN -> (BlockEntityType)BlockEntity.SH_PSD_DOOR_RUNIN_TILE_ENTITY.get();
                case DESTINATION -> (BlockEntityType)BlockEntity.SH_PSD_DOOR_BLANK_TILE_ENTITY.get();
                case BLANK -> (BlockEntityType)BlockEntity.SH_PSD_DOOR_BLANK_TILE_ENTITY.get();
                default -> throw new IllegalStateException("Unknown PSD door top kind: " + top);
            }, pos, state);
        }

        public void readCompoundTag(CompoundTag compoundTag) {
            this.open = compoundTag.getInt("open");
        }

        public void writeCompoundTag(CompoundTag compoundTag) {
            compoundTag.putInt("open", this.open);
        }

        public AABB getRenderBoundingBox() {
            return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        public void setOpen(int open) {
            if (open != this.open) {
                this.open = open;
                this.setChanged();
                this.syncData();
            }
        }

        public float getOpen(float lastFrameDuration) {
            float change = lastFrameDuration * 0.95f;
            this.openClient = Math.abs((float)this.open - 0.1f - this.openClient) < change ? (float)this.open - 0.1f : (this.openClient < (float)this.open ? (this.openClient += change) : (this.openClient -= change));
            return this.openClient / 32.0f;
        }

        public boolean isOpen() {
            return this.open > 0;
        }
    }
}

