package com.xhg78999.shmetro.render;

import com.xhg78999.shmetro.blocks.BlockShanghaiPSDTop;
import com.xhg78999.shmetro.rmg.RMGMain;
import mtr.block.BlockPSDTop;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.DataCache;
import mtr.data.IGui;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderTrains;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;

public class RenderShanghaiPSDTop<T extends BlockShanghaiPSDTop.TileEntityShanghaiPSDTop>
extends BlockEntityRendererMapper<T>
implements IGui,
IBlock {
    private static final float END_FRONT_OFFSET = 1.0f / (Mth.SQRT_OF_TWO * 16.0f);
    private static final float BOTTOM_DIAGONAL_OFFSET = ((float)Math.sqrt(3.0) - 1.0f) / 32.0f;
    private static final float ROOT_TWO_SCALED = Mth.SQRT_OF_TWO / 16.0f;
    private static final float BOTTOM_END_DIAGONAL_OFFSET = END_FRONT_OFFSET - BOTTOM_DIAGONAL_OFFSET / Mth.SQRT_OF_TWO;
    private static final float COLOR_STRIP_START = 0.90625f;
    private static final float COLOR_STRIP_END = 0.9375f;
    protected final float topPadding;
    protected final float bottomPadding;
    protected final float sidePadding;
    private final float z;
    private final boolean transparentWhite;
    private final Property<Integer> arrowDirectionProperty = BlockPSDTop.ARROW_DIRECTION;

    public RenderShanghaiPSDTop(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
        this.z = 0.121875f;
        this.topPadding = 0.46875f;
        this.bottomPadding = 0.09375f;
        this.sidePadding = 0.0078125f;
        this.transparentWhite = true;
    }

    public final void render(T entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        Level world = entity.getLevel();
        if (world != null) {
            long platformId;
            BlockPos pos = entity.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)HorizontalDirectionalBlock.FACING);
            StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations();
            storedMatrixTransformations.add(matricesNew -> {
                matricesNew.translate(0.5 + (double)entity.getBlockPos().getX(), (double)entity.getBlockPos().getY(), 0.5 + (double)entity.getBlockPos().getZ());
                UtilitiesClient.rotateYDegrees((PoseStack)matricesNew, (float)(-facing.toYRot()));
            });
            if (!RenderTrains.shouldNotRender((BlockPos)pos, (int)RenderTrains.maxTrainRenderDistance, (Direction)null) && (platformId = entity.getPlatformId(ClientData.PLATFORMS, (DataCache)ClientData.DATA_CACHE)) != 0L) {
                storedMatrixTransformations.add(matricesNew -> {
                    matricesNew.translate(0.0, 1.0, 0.0);
                    UtilitiesClient.rotateZDegrees((PoseStack)matricesNew, (float)180.0f);
                    matricesNew.translate(-0.5, (double)(-this.getAdditionalOffset(state)), (double)this.z);
                });
                int leftBlocks = this.getTextureNumber((BlockGetter)world, pos, facing, true);
                int rightBlocks = this.getTextureNumber((BlockGetter)world, pos, facing, false);
                int color = RenderShanghaiPSDTop.getShadingColor(facing, -1);
                BlockShanghaiPSDTop.EnumPSDTop renderType = this.getRenderType((BlockGetter)world, pos.relative(facing.getOpposite(), leftBlocks), state);
                if (renderType != BlockShanghaiPSDTop.EnumPSDTop.BLANK && IBlock.getStatePropertySafe((BlockState)state, (Property)SIDE_EXTENDED) != IBlock.EnumSide.SINGLE) {
                    float width = (float)(leftBlocks + rightBlocks + 1) - this.sidePadding * 2.0f;
                    float height = 1.0f - this.topPadding - this.bottomPadding;
                    int arrowDirection = (Integer)IBlock.getStatePropertySafe((BlockState)state, this.arrowDirectionProperty);
                    ResourceLocation resourceLocation = renderType == BlockShanghaiPSDTop.EnumPSDTop.RUNIN ? RMGMain.getRuninCache((long)platformId, (boolean)((arrowDirection & 1) > 0 ? true : false), (boolean)(arrowDirection > 1 ? true : false)).resourceLocation : (renderType == BlockShanghaiPSDTop.EnumPSDTop.DESTINATION ? RMGMain.getDestinationCache((long)platformId, (boolean)((arrowDirection & 1) > 0 ? true : false), (boolean)(arrowDirection > 1 ? true : false), (int)((int)(width / height * 400.0f))).resourceLocation : RMGMain.getRailMapCache((long)platformId, (boolean)((arrowDirection & 1) > 0 ? true : false), (boolean)(arrowDirection > 1 ? true : false), (int)((int)(width / height * 400.0f))).resourceLocation);
                    RenderTrains.scheduleRender((ResourceLocation)resourceLocation, (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                        storedMatrixTransformations.transform(matricesNew);
                        IDrawing.drawTexture((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, (float)(leftBlocks == 0 ? this.sidePadding : 0.0f), (float)this.topPadding, (float)0.0f, (float)(1.0f - (rightBlocks == 0 ? this.sidePadding : 0.0f)), (float)(1.0f - this.bottomPadding), (float)0.0f, (float)(((float)leftBlocks - (leftBlocks == 0 ? 0.0f : this.sidePadding)) / width), (float)0.0f, (float)((width - (float)rightBlocks + (rightBlocks == 0 ? 0.0f : this.sidePadding)) / width), (float)1.0f, (Direction)facing.getOpposite(), (int)color, (int)light);
                        matricesNew.popPose();
                    });
                }
            }
        }
    }

    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    protected float getAdditionalOffset(BlockState state) {
        return 0.0f;
    }

    protected boolean isLeft(BlockState state) {
        return IBlock.getStatePropertySafe((BlockState)state, (Property)SIDE_EXTENDED) == IBlock.EnumSide.LEFT;
    }

    protected boolean isRight(BlockState state) {
        return IBlock.getStatePropertySafe((BlockState)state, (Property)SIDE_EXTENDED) == IBlock.EnumSide.RIGHT;
    }

    protected BlockShanghaiPSDTop.EnumPSDTop getRenderType(BlockGetter world, BlockPos pos, BlockState state) {
        return IBlock.getStatePropertySafe(state, BlockShanghaiPSDTop.KIND);
    }

    private int getTextureNumber(BlockGetter world, BlockPos pos, Direction facing, boolean searchLeft) {
        BlockState state;
        int number = 0;
        Block thisBlock = world.getBlockState(pos).getBlock();
        while ((state = world.getBlockState(pos.relative(searchLeft ? facing.getOpposite() : facing.getClockWise(), number))).getBlock() == thisBlock) {
            boolean isLeft = this.isLeft(state);
            boolean isRight = this.isRight(state);
            if (number != 0 && (searchLeft ? isRight : isLeft)) break;
            ++number;
            if (!(searchLeft ? isLeft : isRight)) continue;
            break;
        }
        return number - 1;
    }

    public static int getShadingColor(Direction facing, int grayscaleColorByte) {
        int colorByte = Math.round((float)(grayscaleColorByte & 0xFF) * (facing.getAxis() == Direction.Axis.X ? 0.75f : 1.0f));
        return 0xFF000000 | (colorByte << 16) + (colorByte << 8) + colorByte;
    }
}

