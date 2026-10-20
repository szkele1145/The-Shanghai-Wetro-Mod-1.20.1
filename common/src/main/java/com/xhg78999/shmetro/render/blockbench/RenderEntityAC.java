package com.xhg78999.shmetro.render.blockbench;

import com.xhg78999.shmetro.blocks.BlockAC;
import com.xhg78999.shmetro.entity.BlockEntityAC;
import com.xhg78999.shmetro.resource.BlockbenchModel;
import com.xhg78999.shmetro.resource.GraphicsCache;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderTrains;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

public class RenderEntityAC
extends BlockEntityRendererMapper<BlockEntityAC>
implements BlockEntityRenderer<BlockEntityAC> {
    private BlockbenchModel model;

    public RenderEntityAC(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    public void render(BlockEntityAC entity, float tick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        Level world = entity.getLevel();
        if (this.model == null) {
            this.model = GraphicsCache.CACHE.models.get("ac");
        }
        // 模型缺失(资源包损坏 / 该模型被移除)时直接跳过渲染:
        // 原实现会在下一行无条件解引用 this.model -> NPE 崩溃客户端。
        if (this.model == null) {
            return;
        }
        if (world != null) {
            BlockPos pos = entity.getBlockPos();
            Direction facing = (Direction)IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockAC.FACING);
            StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations();
            storedMatrixTransformations.add(matricesNew -> {
                matricesNew.translate(0.5 + (double)entity.getBlockPos().getX(), (double)entity.getBlockPos().getY(), 0.5 + (double)entity.getBlockPos().getZ());
                UtilitiesClient.rotateYDegrees((PoseStack)matricesNew, (float)(-facing.toYRot()));
                UtilitiesClient.rotateXDegrees((PoseStack)matricesNew, (float)180.0f);
            });
            RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", "textures/block/ac.png"), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                storedMatrixTransformations.transform(matricesNew);
                this.model.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                matricesNew.popPose();
            });
        }
    }
}

