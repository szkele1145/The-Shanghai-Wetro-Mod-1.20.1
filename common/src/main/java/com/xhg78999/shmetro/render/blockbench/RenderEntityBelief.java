package com.xhg78999.shmetro.render.blockbench;

import com.xhg78999.shmetro.entity.BlockEntityBeliefBeacon;
import com.xhg78999.shmetro.resource.BlockbenchModel;
import com.xhg78999.shmetro.resource.GraphicsCache;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderTrains;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

public class RenderEntityBelief
extends BlockEntityRendererMapper<BlockEntityBeliefBeacon>
implements BlockEntityRenderer<BlockEntityBeliefBeacon> {
    private BlockbenchModel model;

    public RenderEntityBelief(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    public void render(BlockEntityBeliefBeacon entity, float tick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        Level world = entity.getLevel();
        if (this.model == null) {
            this.model = GraphicsCache.CACHE.models.get("belief_beacon");
        }
        if (world != null) {
            BlockPos pos = entity.getBlockPos();
            StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations();
            storedMatrixTransformations.add(matricesNew -> {
                matricesNew.translate(0.5 + (double)entity.getBlockPos().getX(), (double)entity.getBlockPos().getY(), 0.5 + (double)entity.getBlockPos().getZ());
                UtilitiesClient.rotateXDegrees((PoseStack)matricesNew, (float)180.0f);
            });
            RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", "textures/block/belief_beacon.png"), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                storedMatrixTransformations.transform(matricesNew);
                this.model.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                matricesNew.popPose();
            });
        }
    }
}

