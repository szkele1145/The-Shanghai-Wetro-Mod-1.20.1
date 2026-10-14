package com.xhg78999.shmetro.render;

import com.xhg78999.shmetro.blocks.BlockTipBlock;
import com.xhg78999.shmetro.entity.BlockEntityGlobalPosition;
import com.xhg78999.shmetro.resource.GraphicsHelper;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.UtilitiesClient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.Property;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

public class RenderGlobalPosition<T extends BlockEntityGlobalPosition>
extends BlockEntityRendererMapper<T>
implements BlockEntityRenderer<T> {
    private BlockEntityRenderDispatcher dispatcher;

    public RenderGlobalPosition(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
        this.dispatcher = dispatcher;
    }

    public void render(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level world = entity.getLevel();
        if (world != null) {
            BlockPos pos = entity.getBlockPos();
            Direction facing = (Direction)IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockTipBlock.FACING);
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            UtilitiesClient.rotateXDegrees((PoseStack)poseStack, (float)180.0f);
            UtilitiesClient.rotateZDegrees((PoseStack)poseStack, (float)180.0f);
            UtilitiesClient.rotateYDegrees((PoseStack)poseStack, (float)(-facing.toYRot()));
            poseStack.translate(0.0, 0.0, -0.48);
            GraphicsHelper.drawTextFont(poseStack, bufferSource, this.dispatcher, ((BlockEntityGlobalPosition)entity).code, 0.1f, 0.5f, 255, 255, 255, packedLight, 0.02f, -0.02f, 0.02f);
            poseStack.popPose();
        }
    }
}

