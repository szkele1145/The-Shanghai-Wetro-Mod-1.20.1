package com.xhg78999.shmetro.render;

import com.xhg78999.shmetro.blocks.BlockShanghaiPSDDoor;
import mtr.MTRClient;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.IBlock;
import mtr.data.IGui;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.ModelDataWrapper;
import mtr.mappings.ModelMapper;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderTrains;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.Model;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;

public class RenderShanghaiPSDDoor<T extends BlockShanghaiPSDDoor.TileEntitySHPSDDoor>
extends BlockEntityRendererMapper<T>
implements IGui,
IBlock {
    private static final ModelSingleCube MODEL_PSD = new ModelSingleCube(36, 18, 0, 0, 0, 16, 16, 2);
    private static final ModelSingleCube MODEL_PSD_END_LEFT_1 = new ModelSingleCube(20, 18, 0, 0, 0, 8, 16, 2);
    private static final ModelSingleCube MODEL_PSD_END_RIGHT_1 = new ModelSingleCube(20, 18, 8, 0, 0, 8, 16, 2);
    private static final ModelSingleCube MODEL_PSD_END_LEFT_2 = new ModelSingleCube(20, 18, 8, 0, 2, 8, 16, 2);
    private static final ModelSingleCube MODEL_PSD_END_RIGHT_2 = new ModelSingleCube(20, 18, 0, 0, 2, 8, 16, 2);
    private static final ModelSingleCube MODEL_PSD_LIGHT_LEFT = new ModelSingleCube(16, 16, 0, -1, 5, 1, 1, 1);
    private static final ModelSingleCube MODEL_PSD_LIGHT_RIGHT = new ModelSingleCube(16, 16, 15, -1, 5, 1, 1, 1);

    public RenderShanghaiPSDDoor(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    public void render(T entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        Level world = entity.getLevel();
        if (world != null) {
            BlockPos pos = entity.getBlockPos();
            Direction facing = (Direction)IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockPSDAPGDoorBase.FACING);
            boolean side = IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockPSDAPGDoorBase.SIDE) == IBlock.EnumSide.RIGHT;
            boolean half = IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockPSDAPGDoorBase.HALF) == DoubleBlockHalf.UPPER;
            boolean end = (Boolean)IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockPSDAPGDoorBase.END);
            float open = Math.min(((BlockShanghaiPSDDoor.TileEntitySHPSDDoor)((Object)entity)).getOpen(MTRClient.getLastFrameDuration()), 1.0f);
            StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations();
            storedMatrixTransformations.add(matricesNew -> {
                matricesNew.translate(0.5 + (double)entity.getBlockPos().getX(), (double)entity.getBlockPos().getY(), 0.5 + (double)entity.getBlockPos().getZ());
                UtilitiesClient.rotateYDegrees((PoseStack)matricesNew, (float)(-facing.toYRot()));
                UtilitiesClient.rotateXDegrees((PoseStack)matricesNew, (float)180.0f);
            });
            StoredMatrixTransformations storedMatrixTransformationsLight = storedMatrixTransformations.copy();
            if (half) {
                RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation(String.format("mtr:textures/block/light_%s.png", open > 0.0f ? "on" : "off")), (boolean)false, (RenderTrains.QueuedRenderLayer)(open > 0.0f ? RenderTrains.QueuedRenderLayer.LIGHT : RenderTrains.QueuedRenderLayer.EXTERIOR), (matricesNew, vertexConsumer) -> {
                    storedMatrixTransformationsLight.transform(matricesNew);
                    (side ? MODEL_PSD_LIGHT_RIGHT : MODEL_PSD_LIGHT_LEFT).renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                    matricesNew.popPose();
                });
            }
            if (end) {
                RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation(String.format("shmetro:textures/block/sh_psd_door_end_%s_%s_2_1.png", half ? "top" : "bottom", side ? "right" : "left")), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                    storedMatrixTransformationsLight.transform(matricesNew);
                    matricesNew.translate((double)(open / 2.0f * (float)(side ? -1 : 1)), 0.0, 0.0);
                    (side ? MODEL_PSD_END_RIGHT_2 : MODEL_PSD_END_LEFT_2).renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                    matricesNew.popPose();
                });
            }
            storedMatrixTransformations.add(matricesNew -> matricesNew.translate((double)(open * (float)(side ? -1 : 1)), 0.0, 0.0));
            if (end) {
                RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation(String.format("shmetro:textures/block/sh_psd_door_end_%s_%s_1_1.png", half ? "top" : "bottom", side ? "right" : "left")), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                    storedMatrixTransformations.transform(matricesNew);
                    (side ? MODEL_PSD_END_RIGHT_1 : MODEL_PSD_END_LEFT_1).renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                    matricesNew.popPose();
                });
            } else {
                RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation(String.format("shmetro:textures/block/sh_psd_door_%s_%s_1.png", half ? "top" : "bottom", side ? "right" : "left")), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                    storedMatrixTransformations.transform(matricesNew);
                    MODEL_PSD.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                    matricesNew.popPose();
                });
            }
        }
    }

    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    private static class ModelSingleCube
    extends EntityModel<Entity> {
        private final ModelMapper cube;

        private ModelSingleCube(int textureWidth, int textureHeight, int x, int y, int z, int length, int height, int depth) {
            ModelDataWrapper modelDataWrapper = new ModelDataWrapper((Model)this, textureWidth, textureHeight);
            this.cube = new ModelMapper(modelDataWrapper);
            this.cube.texOffs(0, 0).addBox((float)(x - 8), (float)(y - 16), (float)(z - 8), length, height, depth, 0.0f, false);
            modelDataWrapper.setModelPart(textureWidth, textureHeight);
            this.cube.setModelPart();
        }

        public void renderToBuffer(PoseStack matrices, VertexConsumer vertices, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
            this.cube.render(matrices, vertices, 0.0f, 0.0f, 0.0f, packedLight, packedOverlay);
        }

        public void setupAnim(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        }
    }
}

