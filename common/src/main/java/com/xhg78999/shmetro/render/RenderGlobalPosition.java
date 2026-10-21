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
        // 【1.1.61 加空值防护】code 为空(还没编号 / 刚被清空)时直接跳过绘制。
        // 原来是把可能为 null 的 code 直接丢给 GraphicsHelper.drawText,
        // 而本方法在渲染热路径上没有 try/catch —— 一次 NPE 就会打断整个
        // 渲染批次。空编号本来就没有文字可画, 跳过即可。
        String code = entity.code;
        if (code == null || code.isEmpty()) {
            return;
        }
        if (world != null) {
            BlockPos pos = entity.getBlockPos();
            Direction facing = (Direction)IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockTipBlock.FACING);
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            UtilitiesClient.rotateXDegrees((PoseStack)poseStack, (float)180.0f);
            UtilitiesClient.rotateZDegrees((PoseStack)poseStack, (float)180.0f);
            UtilitiesClient.rotateYDegrees((PoseStack)poseStack, (float)(-facing.toYRot()));
            poseStack.translate(0.0, 0.0, -0.48);
            GraphicsHelper.drawTextFont(poseStack, bufferSource, this.dispatcher, code, 0.1f, 0.5f, 255, 255, 255, packedLight, 0.02f, -0.02f, 0.02f);
            poseStack.popPose();
        }
    }
}

