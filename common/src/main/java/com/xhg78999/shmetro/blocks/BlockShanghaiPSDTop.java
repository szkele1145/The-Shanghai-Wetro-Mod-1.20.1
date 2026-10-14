package com.xhg78999.shmetro.blocks;

import net.minecraft.world.level.block.SoundType;
import com.xhg78999.shmetro.BlockEntity;
import com.xhg78999.shmetro.Blocks;
import com.xhg78999.shmetro.Items;
import com.xhg78999.shmetro.blocks.BlockShanghaiPSDDoor;
import com.xhg78999.shmetro.blocks.BlockShanghaiPSDEnd;
import com.xhg78999.shmetro.blocks.BlockShanghaiPSDGlass;
import mtr.block.BlockPSDAPGBase;
import mtr.block.BlockPSDAPGGlassEndBase;
import mtr.block.BlockPSDDoor;
import mtr.block.BlockPSDGlass;
import mtr.block.BlockPSDGlassEnd;
import mtr.block.BlockPSDTop;
import mtr.block.IBlock;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import mtr.mappings.Text;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockShanghaiPSDTop
extends BlockDirectionalMapper
implements EntityBlockMapper,
IBlock {
    private static final float PERSISTENT_OFFSET = 7.5f;
    public static final float PERSISTENT_OFFSET_SMALL = 0.46875f;
    public static final BooleanProperty AIR_LEFT = BooleanProperty.create((String)"air_left");
    public static final BooleanProperty AIR_RIGHT = BooleanProperty.create((String)"air_right");
    public static final IntegerProperty ARROW_DIRECTION = IntegerProperty.create((String)"propagate_property", (int)0, (int)3);
    public static final EnumProperty<EnumPSDTop> KIND = EnumProperty.create((String)"kind", EnumPSDTop.class);
    public static final EnumProperty<BlockPSDTop.EnumPersistent> PERSISTENT = EnumProperty.create((String)"persistent", BlockPSDTop.EnumPersistent.class);

    public BlockShanghaiPSDTop() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW).sound(SoundType.METAL).noLootTable().destroyTime(2.0f).noOcclusion());
    }

    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        return IBlock.checkHoldingItem((Level)world, (Player)player, item -> {
            if (item == mtr.Items.BRUSH.get()) {
                world.setBlockAndUpdate(pos, (BlockState)state.cycle((Property)ARROW_DIRECTION));
                this.propagate(world, pos, ((Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING)).getClockWise(), (Property)ARROW_DIRECTION, 1);
                this.propagate(world, pos, ((Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING)).getCounterClockWise(), (Property)ARROW_DIRECTION, 1);
            } else {
                boolean shouldBePersistent = IBlock.getStatePropertySafe((BlockState)state, PERSISTENT) == BlockPSDTop.EnumPersistent.NONE;
                player.displayClientMessage((Component)Text.translatable((String)(shouldBePersistent ? "gui.shmetro.psd_top_cut" : "gui.shmetro.psd_top_combine"), (Object[])new Object[0]), true);
                this.setState(world, pos, shouldBePersistent);
                this.propagate(world, pos, ((Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING)).getClockWise(), offsetPos -> this.setState(world, (BlockPos)offsetPos, shouldBePersistent), 1);
                this.propagate(world, pos, ((Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING)).getCounterClockWise(), offsetPos -> this.setState(world, (BlockPos)offsetPos, shouldBePersistent), 1);
            }
        }, (Runnable)null, (Item[])new Item[]{(Item)mtr.Items.BRUSH.get(), net.minecraft.world.item.Items.SHEARS});
    }

    private void setState(Level world, BlockPos pos, boolean shouldBePersistent) {
        world.setBlockAndUpdate(pos, (BlockState)world.getBlockState(pos).setValue(PERSISTENT, BlockPSDTop.EnumPersistent.NONE));
        Block blockBelow = world.getBlockState(pos.below()).getBlock();
        if (blockBelow instanceof BlockShanghaiPSDDoor || blockBelow instanceof BlockShanghaiPSDGlass || blockBelow instanceof BlockShanghaiPSDEnd) {
            if (shouldBePersistent) {
                world.setBlockAndUpdate(pos, (BlockState)world.getBlockState(pos).setValue(PERSISTENT, blockBelow instanceof BlockPSDDoor ? BlockPSDTop.EnumPersistent.ARROW : (blockBelow instanceof BlockPSDGlass ? BlockPSDTop.EnumPersistent.ROUTE : BlockPSDTop.EnumPersistent.BLANK)));
            } else {
                world.setBlockAndUpdate(pos, (BlockState)world.getBlockState(pos).setValue(PERSISTENT, BlockPSDTop.EnumPersistent.NONE));
            }
        }
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    public Item asItem() {
        return (Item)Items.SH_PSD_DOOR_RUNIN.get();
    }

    public ItemStack getCloneItemStack(BlockGetter blockGetter, BlockPos blockPos, BlockState blockState) {
        return new ItemStack((ItemLike)this.asItem());
    }

    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        Block blockDown = world.getBlockState(pos.below()).getBlock();
        if (blockDown instanceof BlockPSDAPGBase) {
            blockDown.playerWillDestroy(world, pos.below(), world.getBlockState(pos.below()), player);
            world.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        super.playerWillDestroy(world, pos, state, player);
    }

    public BlockState updateShape(BlockState state, Direction direction, BlockState newState, LevelAccessor world, BlockPos pos, BlockPos posFrom) {
        return direction == Direction.DOWN && IBlock.getStatePropertySafe((BlockState)state, PERSISTENT) == BlockPSDTop.EnumPersistent.NONE && !(newState.getBlock() instanceof BlockPSDAPGBase) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : BlockShanghaiPSDTop.getActualState((BlockGetter)world, pos);
    }

    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
        VoxelShape baseShape = IBlock.getVoxelShapeByDirection((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)6.0, (Direction)((Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING)));
        boolean airLeft = (Boolean)IBlock.getStatePropertySafe((BlockState)state, (Property)AIR_LEFT);
        boolean airRight = (Boolean)IBlock.getStatePropertySafe((BlockState)state, (Property)AIR_RIGHT);
        return !airLeft && !airRight ? baseShape : BlockPSDAPGGlassEndBase.getEndOutlineShape((VoxelShape)baseShape, (BlockState)state, (int)16, (int)6, (boolean)airLeft, (boolean)airRight);
    }

    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    public PushReaction getPushReaction(BlockState blockState) {
        return PushReaction.BLOCK;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, SIDE_EXTENDED, AIR_LEFT, AIR_RIGHT, ARROW_DIRECTION, PERSISTENT, KIND});
    }

    public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityShanghaiPSDTop(pos, state);
    }

    public static BlockState getActualState(BlockGetter world, BlockPos pos) {
        BlockState oldState;
        Direction facing = null;
        IBlock.EnumSide side = null;
        boolean airLeft = false;
        boolean airRight = false;
        BlockState stateBelow = world.getBlockState(pos.below());
        Block blockBelow = stateBelow.getBlock();
        if (blockBelow instanceof BlockShanghaiPSDGlass || blockBelow instanceof BlockShanghaiPSDDoor || blockBelow instanceof BlockShanghaiPSDEnd) {
            side = blockBelow instanceof BlockShanghaiPSDDoor ? (IBlock.EnumSide)IBlock.getStatePropertySafe((BlockState)stateBelow, (Property)SIDE) : (IBlock.EnumSide)IBlock.getStatePropertySafe((BlockState)stateBelow, (Property)SIDE_EXTENDED);
            if (blockBelow instanceof BlockShanghaiPSDEnd) {
                if (IBlock.getStatePropertySafe((BlockState)stateBelow, (Property)BlockPSDGlassEnd.TOUCHING_LEFT) == BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide.AIR) {
                    airLeft = true;
                }
                if (IBlock.getStatePropertySafe((BlockState)stateBelow, (Property)BlockPSDGlassEnd.TOUCHING_RIGHT) == BlockPSDAPGGlassEndBase.EnumPSDAPGGlassEndSide.AIR) {
                    airRight = true;
                }
            }
            facing = (Direction)IBlock.getStatePropertySafe((BlockState)stateBelow, (Property)FACING);
        }
        BlockState newState = (BlockState)((BlockState)((oldState = world.getBlockState(pos)).getBlock() instanceof BlockShanghaiPSDTop ? oldState : ((Block)Blocks.SH_PSD_TOP.get()).defaultBlockState()).setValue((Property)AIR_LEFT, (Comparable)Boolean.valueOf(airLeft))).setValue((Property)AIR_RIGHT, (Comparable)Boolean.valueOf(airRight));
        if (facing != null) {
            newState = (BlockState)newState.setValue((Property)FACING, (Comparable)facing);
        }
        if (side != null) {
            newState = (BlockState)newState.setValue((Property)SIDE_EXTENDED, (Comparable)side);
        }
        return newState;
    }

    public static class TileEntityShanghaiPSDTop
    extends BlockPSDTop.TileEntityRouteBase {
        public TileEntityShanghaiPSDTop(BlockPos pos, BlockState state) {
            super((BlockEntityType)BlockEntity.SH_PSD_TOP_TILE_ENTITY.get(), pos, state);
        }
    }

    public static enum EnumPSDTop implements StringRepresentable
    {
        RAILMAP("railmap"),
        RUNIN("runin"),
        DESTINATION("destination"),
        BLANK("blank");

        private final String name;

        private EnumPSDTop(String nameIn) {
            this.name = nameIn;
        }

        public String getSerializedName() {
            return this.name;
        }
    }
}

