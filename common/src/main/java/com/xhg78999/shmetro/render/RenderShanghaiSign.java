package com.xhg78999.shmetro.render;

import com.xhg78999.shmetro.blocks.BlockShanghaiSign;
import mtr.block.BlockStationNameBase;
import mtr.block.IBlock;
import mtr.client.CustomResources;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderRailwaySign;
import mtr.render.RenderTrains;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;

public class RenderShanghaiSign<T extends BlockShanghaiSign.TileEntityRailwaySign>
extends BlockEntityRendererMapper<T>
implements IBlock,
IGui,
IDrawing {
    public static final int ARGB_SP_GREEN = 0x223533;

    public RenderShanghaiSign(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    public void render(T entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        Level world = entity.getLevel();
        if (world == null) {
            return;
        }
        BlockPos pos = entity.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockShanghaiSign)) {
            return;
        }
        BlockShanghaiSign block = (BlockShanghaiSign)state.getBlock();
        if (((BlockShanghaiSign.TileEntityRailwaySign)((Object)entity)).getSignIds().length != block.length) {
            return;
        }
        Direction facing = (Direction)IBlock.getStatePropertySafe((BlockState)state, (Property)BlockStationNameBase.FACING);
        String[] signIds = ((BlockShanghaiSign.TileEntityRailwaySign)((Object)entity)).getSignIds();
        boolean renderBackground = false;
        int backgroundColor = 0;
        for (String signId : signIds) {
            CustomResources.CustomSign sign;
            if (signId == null || (sign = RenderRailwaySign.getSign((String)signId)) == null) continue;
            renderBackground = true;
            if (sign.backgroundColor == 0) continue;
            backgroundColor = sign.backgroundColor;
            break;
        }
        StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations();
        storedMatrixTransformations.add(matricesNew -> {
            matricesNew.translate(0.5 + (double)entity.getBlockPos().getX(), 0.53125 + (double)entity.getBlockPos().getY(), 0.5 + (double)entity.getBlockPos().getZ());
            UtilitiesClient.rotateYDegrees((PoseStack)matricesNew, (float)(-facing.toYRot()));
            UtilitiesClient.rotateZDegrees((PoseStack)matricesNew, (float)180.0f);
            UtilitiesClient.rotateXDegrees((PoseStack)matricesNew, (float)22.5f);
            matricesNew.translate((double)((float)block.getXStart() / 16.0f) - 0.5, -0.05, -0.26875000009313227);
        });
        matrices.pushPose();
        matrices.translate(0.5, 0.53125, 0.5);
        UtilitiesClient.rotateXDegrees((PoseStack)matrices, (float)22.5f);
        UtilitiesClient.rotateYDegrees((PoseStack)matrices, (float)(-facing.toYRot()));
        UtilitiesClient.rotateZDegrees((PoseStack)matrices, (float)180.0f);
        matrices.translate((double)((float)block.getXStart() / 16.0f) - 0.5, -0.05, 0.06875000009313226);
        if (renderBackground) {
            int newBackgroundColor = backgroundColor | 0x223533;
            RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("mtr:textures/block/white.png"), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.LIGHT, (matricesNew, vertexConsumer) -> {
                storedMatrixTransformations.transform(matricesNew);
                IDrawing.drawTexture((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, (float)0.0f, (float)0.0f, (float)0.003125f, (float)(0.5f * (float)signIds.length), (float)0.5f, (float)0.003125f, (Direction)facing, (int)newBackgroundColor, (int)0xF000F0);
                matricesNew.popPose();
            });
        }
        for (int i = 0; i < signIds.length; ++i) {
            if (signIds[i] == null) continue;
            RenderRailwaySign.drawSign((PoseStack)matrices, (MultiBufferSource)vertexConsumers, (StoredMatrixTransformations)storedMatrixTransformations, (Font)Minecraft.getInstance().font, (BlockPos)pos, (String)signIds[i], (float)(0.5f * (float)i), (float)0.0f, (float)0.5f, (float)RenderRailwaySign.getMaxWidth((String[])signIds, (int)i, (boolean)false), (float)RenderRailwaySign.getMaxWidth((String[])signIds, (int)i, (boolean)true), ((BlockShanghaiSign.TileEntityRailwaySign)((Object)entity)).getSelectedIds(), (Direction)facing, (int)(backgroundColor | 0x223533), (textureId, x, y, size, flipTexture) -> RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation(textureId.toString()), (boolean)true, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.LIGHT_TRANSLUCENT, (matricesNew, vertexConsumer) -> {
                storedMatrixTransformations.transform(matricesNew);
                IDrawing.drawTexture((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, (float)x, (float)y, (float)size, (float)size, (float)(flipTexture ? 1.0f : 0.0f), (float)0.0f, (float)(flipTexture ? 0.0f : 1.0f), (float)1.0f, (Direction)facing, (int)-1, (int)0xF000F0);
                matricesNew.popPose();
            }));
        }
        matrices.popPose();
    }

    @FunctionalInterface
    public static interface DrawTexture {
        public void drawTexture(ResourceLocation var1, float var2, float var3, float var4, boolean var5);
    }
}

