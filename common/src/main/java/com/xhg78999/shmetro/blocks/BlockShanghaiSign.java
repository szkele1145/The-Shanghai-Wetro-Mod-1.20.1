package com.xhg78999.shmetro.blocks;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.SoundType;
import com.xhg78999.shmetro.BlockEntity;
import com.xhg78999.shmetro.Blocks;
import com.xhg78999.shmetro.network.SHMPacketTrainDataGuiServer;
import com.xhg78999.shmetro.utils.WallPlacementUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mtr.RegistryObject;
import mtr.block.IBlock;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.BlockEntityClientSerializableMapper;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import mtr.mappings.Text;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockShanghaiSign
extends BlockDirectionalMapper
implements EntityBlockMapper,
IBlock {
    public final int length;
    public final boolean isOdd;
    public static final float SMALL_SIGN_PERCENTAGE = 0.75f;

    public BlockShanghaiSign(int length, boolean isOdd) {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLUE).sound(SoundType.METAL).noLootTable().destroyTime(2.0f).lightLevel(state -> 15));
        this.length = length;
        this.isOdd = isOdd;
    }

    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        return IBlock.checkHoldingBrush((Level)level, (Player)player, () -> {
            BlockPos checkPos;
            Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)blockState, (Property)FACING);
            Direction hitSide = blockHitResult.getDirection();
            if ((hitSide == facing || hitSide == facing.getOpposite()) && (checkPos = this.findEndWithDirection(level, blockPos, hitSide.getOpposite(), false)) != null) {
                SHMPacketTrainDataGuiServer.openSHMSignScreenS2C((ServerPlayer)player, checkPos);
            }
        });
    }

    public BlockState updateShape(BlockState state, Direction direction, BlockState newState, LevelAccessor world, BlockPos pos, BlockPos posFrom) {
        boolean isNext;
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)FACING);
        boolean bl = isNext = direction == facing.getClockWise() || state.is((Block)Blocks.SHM_SIGN_MIDDLE.get()) && direction == facing.getOpposite();
        if (isNext && !(newState.getBlock() instanceof BlockShanghaiSign)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        // 【1.1.45 修正】此方法原先被误命名为 rotate(BlockPlaceContext)。
        // 上游 1.19.4 原件里它是 method_9605 —— 经 intermediary 映射查证,
        // method_9605 == getStateForPlacement (BlockPlaceContext)BlockState,
        // 而 Block.rotate 的 intermediary 是 method_26388, 两者不同名。
        // CFR 反编译时把 method_9605 错还原成了 "rotate", 移植时照抄,
        // 导致本方法不覆盖任何基类方法 —— MC 永远不会调用它:
        //   1) 指示牌放置时 FACING 恒为默认的 NORTH (点哪面墙都朝北);
        //   2) isReplaceable 的空间预检彻底失效, 指示牌可以塞进实体方块里;
        //   3) setPlacedBy 里沿 facing.getClockWise() 延伸整排指示牌的逻辑
        //      全部按 NORTH 方向展开。
        // 改回 getStateForPlacement 后 MC 才会真正调用它。
        //
        // 另外上游直接用 ctx.getClickedFace(): 点中顶面/底面时返回 UP/DOWN,
        // 而 FACING 是水平方向属性 (HorizontalDirectionalBlock.FACING 只含
        // 四个水平方向), setValue 会抛 IllegalArgumentException。
        // 指示牌是壁挂薄板, 点顶/底面在几何上毫无意义, 直接拒绝放置。
        Direction facing = WallPlacementUtil.getWallFacingOrNull(blockPlaceContext);
        if (facing == null) {
            return null;
        }
        return IBlock.isReplaceable((BlockPlaceContext)blockPlaceContext, (Direction)facing.getClockWise(), (int)(this.getMiddleLength() + 2)) ? (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)facing) : null;
    }

    public void playerWillDestroy(Level level, BlockPos blockPos, BlockState blockState, Player player) {
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)blockState, (Property)FACING);
        BlockPos checkPos = this.findEndWithDirection(level, blockPos, facing, true);
        if (checkPos != null) {
            IBlock.onBreakCreative((Level)level, (Player)player, (BlockPos)checkPos);
        }
        super.playerWillDestroy(level, blockPos, blockState, player);
    }

    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, LivingEntity livingEntity, ItemStack itemStack) {
        if (!level.isClientSide) {
            Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)blockState, (Property)FACING);
            for (int i = 1; i <= this.getMiddleLength(); ++i) {
                level.setBlock(blockPos.relative(facing.getClockWise(), i), (BlockState)((Block)Blocks.SHM_SIGN_MIDDLE.get()).defaultBlockState().setValue((Property)FACING, (Comparable)facing), 3);
            }
            level.setBlock(blockPos.relative(facing.getClockWise(), this.getMiddleLength() + 1), (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)facing.getOpposite()), 3);
            level.updateNeighborsAt(blockPos, net.minecraft.world.level.block.Blocks.AIR);
            blockState.updateNeighbourShapes((LevelAccessor)level, blockPos, 3);
        }
    }

    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return Shapes.block();
    }

    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    public String getDescriptionId() {
        return "block.shmetro.shm_sign";
    }

    public void appendHoverText(ItemStack itemStack, BlockGetter blockGetter, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add((Component)Text.translatable((String)"tooltip.mtr.railway_sign_length", (Object[])new Object[]{this.length}).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
        tooltip.add((Component)Text.translatable((String)(this.isOdd ? "tooltip.mtr.railway_sign_odd" : "tooltip.mtr.railway_sign_even"), (Object[])new Object[0]).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
    }

    public BlockEntityMapper createBlockEntity(BlockPos blockPos, BlockState blockState) {
        if (this == Blocks.SHM_SIGN_MIDDLE.get()) {
            return null;
        }
        return new TileEntityRailwaySign(this.length, this.isOdd, blockPos, blockState);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }

    public int getXStart() {
        switch (this.length % 4) {
            case 1: {
                return this.isOdd ? 4 : 12;
            }
            case 2: {
                return this.isOdd ? 16 : 8;
            }
            case 3: {
                return this.isOdd ? 12 : 4;
            }
        }
        return this.isOdd ? 8 : 16;
    }

    private int getMiddleLength() {
        return (this.length - (4 - this.getXStart() / 4)) / 2;
    }

    private BlockPos findEndWithDirection(Level world, BlockPos startPos, Direction direction, boolean allowOpposite) {
        int i = 0;
        while (true) {
            BlockPos checkPos;
            BlockState checkState;
            if ((checkState = world.getBlockState(checkPos = startPos.relative(direction.getOpposite(), i))).getBlock() instanceof BlockShanghaiSign) {
                Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)checkState, (Property)FACING);
                if (!checkState.is((Block)Blocks.SHM_SIGN_MIDDLE.get()) && (facing == direction || allowOpposite && facing == direction.getOpposite())) {
                    return checkPos;
                }
            } else {
                return null;
            }
            ++i;
        }
    }

    public static class TileEntityRailwaySign
    extends BlockEntityClientSerializableMapper {
        private final Set<Long> selectedIds;
        private final String[] signIds;
        private static final String KEY_SELECTED_IDS = "shm_selected_ids";
        private static final String KEY_SIGN_LENGTH = "shm_sign_length";

        public TileEntityRailwaySign(int length, boolean isOdd, BlockPos pos, BlockState state) {
            super(TileEntityRailwaySign.getType(length, isOdd), pos, state);
            this.signIds = new String[length];
            this.selectedIds = new HashSet<Long>();
        }

        @Environment(value=EnvType.CLIENT)
        public double getViewDistance() {
            return 128.0;
        }

        public void readCompoundTag(CompoundTag compoundTag) {
            this.selectedIds.clear();
            Arrays.stream(compoundTag.getLongArray(KEY_SELECTED_IDS)).forEach(this.selectedIds::add);
            for (int i = 0; i < this.signIds.length; ++i) {
                String signId = compoundTag.getString(KEY_SIGN_LENGTH + i);
                this.signIds[i] = signId.isEmpty() ? null : signId;
            }
        }

        public void writeCompoundTag(CompoundTag compoundTag) {
            compoundTag.putLongArray(KEY_SELECTED_IDS, new ArrayList<Long>(this.selectedIds));
            for (int i = 0; i < this.signIds.length; ++i) {
                compoundTag.putString(KEY_SIGN_LENGTH + i, this.signIds[i] == null ? "" : this.signIds[i]);
            }
        }

        public void setData(Set<Long> selectedIds, String[] signTypes) {
            this.selectedIds.clear();
            this.selectedIds.addAll(selectedIds);
            if (this.signIds.length == signTypes.length) {
                System.arraycopy(signTypes, 0, this.signIds, 0, signTypes.length);
            }
            this.setChanged();
            this.syncData();
        }

        public Set<Long> getSelectedIds() {
            return this.selectedIds;
        }

        public String[] getSignIds() {
            return this.signIds;
        }

        private static final Map<Integer, RegistryObject<? extends BlockEntityType<?>>> SIGN_ODD = new HashMap<>();
        private static final Map<Integer, RegistryObject<? extends BlockEntityType<?>>> SIGN_EVEN = new HashMap<>();
        static {
            SIGN_ODD.put(2, BlockEntity.SHM_SIGN_2_ODD_TILE_ENTITY);
            SIGN_ODD.put(3, BlockEntity.SHM_SIGN_3_ODD_TILE_ENTITY);
            SIGN_ODD.put(4, BlockEntity.SHM_SIGN_4_ODD_TILE_ENTITY);
            SIGN_ODD.put(5, BlockEntity.SHM_SIGN_5_ODD_TILE_ENTITY);
            SIGN_ODD.put(6, BlockEntity.SHM_SIGN_6_ODD_TILE_ENTITY);
            SIGN_ODD.put(7, BlockEntity.SHM_SIGN_7_ODD_TILE_ENTITY);
            SIGN_ODD.put(8, BlockEntity.SHM_SIGN_8_ODD_TILE_ENTITY);
            SIGN_ODD.put(9, BlockEntity.SHM_SIGN_9_ODD_TILE_ENTITY);
            SIGN_ODD.put(10, BlockEntity.SHM_SIGN_10_ODD_TILE_ENTITY);
            SIGN_ODD.put(11, BlockEntity.SHM_SIGN_11_ODD_TILE_ENTITY);
            SIGN_ODD.put(12, BlockEntity.SHM_SIGN_12_ODD_TILE_ENTITY);
            SIGN_ODD.put(13, BlockEntity.SHM_SIGN_13_ODD_TILE_ENTITY);
            SIGN_EVEN.put(2, BlockEntity.SHM_SIGN_2_EVEN_TILE_ENTITY);
            SIGN_EVEN.put(3, BlockEntity.SHM_SIGN_3_EVEN_TILE_ENTITY);
            SIGN_EVEN.put(4, BlockEntity.SHM_SIGN_4_EVEN_TILE_ENTITY);
            SIGN_EVEN.put(5, BlockEntity.SHM_SIGN_5_EVEN_TILE_ENTITY);
            SIGN_EVEN.put(6, BlockEntity.SHM_SIGN_6_EVEN_TILE_ENTITY);
            SIGN_EVEN.put(7, BlockEntity.SHM_SIGN_7_EVEN_TILE_ENTITY);
            SIGN_EVEN.put(8, BlockEntity.SHM_SIGN_8_EVEN_TILE_ENTITY);
            SIGN_EVEN.put(9, BlockEntity.SHM_SIGN_9_EVEN_TILE_ENTITY);
            SIGN_EVEN.put(10, BlockEntity.SHM_SIGN_10_EVEN_TILE_ENTITY);
            SIGN_EVEN.put(11, BlockEntity.SHM_SIGN_11_EVEN_TILE_ENTITY);
            SIGN_EVEN.put(12, BlockEntity.SHM_SIGN_12_EVEN_TILE_ENTITY);
            SIGN_EVEN.put(13, BlockEntity.SHM_SIGN_13_EVEN_TILE_ENTITY);
        }

        private static BlockEntityType<?> getType(int length, boolean isOdd) {
            RegistryObject<? extends BlockEntityType<?>> ro = isOdd ? SIGN_ODD.get(length) : SIGN_EVEN.get(length);
            return ro == null ? null : ro.get();
        }
    }
}

