package com.xhg78999.shmetro.render;

import com.xhg78999.shmetro.resource.GraphicsCache;
import mtr.MTRClient;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.block.IBlock;
import mtr.data.IGui;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderPSDAPGDoor;
import mtr.render.RenderTrains;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;

public class RenderERailingDoor<T>
extends RenderPSDAPGDoor
implements IGui,
IBlock {
    private final int type;

    public RenderERailingDoor(BlockEntityRenderDispatcher dispatcher, int type) {
        super(dispatcher, type);
        this.type = type;
    }

    public void render(BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        Level world = entity.getLevel();
        if (world != null) {
            BlockPos pos = entity.getBlockPos();
            boolean half = IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockPSDAPGDoorBase.HALF) == DoubleBlockHalf.UPPER;
            Direction facing = (Direction)IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockPSDAPGDoorBase.FACING);
            boolean side = IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockPSDAPGDoorBase.SIDE) == IBlock.EnumSide.RIGHT;
            boolean unlocked = (Boolean)IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockPSDAPGDoorBase.UNLOCKED);
            float open = Math.min(entity.getOpen(MTRClient.getLastFrameDuration()), 1.0f);
            StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations();
            storedMatrixTransformations.add(matricesNew -> {
                matricesNew.translate(0.5 + (double)entity.getBlockPos().getX(), (double)entity.getBlockPos().getY(), 0.5 + (double)entity.getBlockPos().getZ());
                UtilitiesClient.rotateYDegrees((PoseStack)matricesNew, (float)(-facing.toYRot()));
                UtilitiesClient.rotateXDegrees((PoseStack)matricesNew, (float)180.0f);
            });
            storedMatrixTransformations.add(matricesNew -> matricesNew.translate((double)(open * (float)(side ? -1 : 1)), 0.0, 0.0));
            if (!half) {
                RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation(String.format("shmetro:textures/block/erailing_%s_%s.png", this.type == 114514 ? "1" : "2", side ? "right" : "left")), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                    storedMatrixTransformations.transform(matricesNew);
                    ((EntityModel)GraphicsCache.CACHE.models.get("erailing_door")).renderToBuffer(matricesNew, vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                    matricesNew.popPose();
                });
            }
        }
    }
}

