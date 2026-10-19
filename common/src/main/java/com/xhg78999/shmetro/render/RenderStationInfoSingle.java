package com.xhg78999.shmetro.render;

import com.xhg78999.shmetro.blocks.BlockStationInfoBoardSingle;
import com.xhg78999.shmetro.entity.BlockEntityStationInfoBoardSingle;
import com.xhg78999.shmetro.resource.GraphicsHelper;
import com.xhg78999.shmetro.utils.StringUtil;
import java.util.Set;
import mtr.block.IBlock;
import mtr.client.ClientData;
import mtr.data.DataCache;
import mtr.data.RailwayData;
import mtr.data.Station;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.Text;
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

public class RenderStationInfoSingle<T extends BlockEntityStationInfoBoardSingle>
extends BlockEntityRendererMapper<T>
implements BlockEntityRenderer<T> {
    private BlockEntityRenderDispatcher dispatcher;

    public RenderStationInfoSingle(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
        this.dispatcher = dispatcher;
    }

    public void render(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level world = entity.getLevel();
        if (world != null) {
            BlockPos pos = entity.getBlockPos();
            Direction facing = (Direction)IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockStationInfoBoardSingle.FACING);
            Station station = RailwayData.getStation((Set)ClientData.STATIONS, (DataCache)ClientData.DATA_CACHE, (BlockPos)pos);
            String fullName = station == null ? Text.translatable((String)"gui.mtr.untitled", (Object[])new Object[0]).getString() : station.name;
            String[] cjkAndEng = StringUtil.getCjkAndEngMain(fullName);
            String cjkName = cjkAndEng[0];
            String engName = cjkAndEng[1];
            poseStack.translate(0.5, 0.5, 0.5);
            UtilitiesClient.rotateXDegrees((PoseStack)poseStack, (float)180.0f);
            UtilitiesClient.rotateYDegrees((PoseStack)poseStack, (float)facing.toYRot());
            poseStack.scale(0.0625f, 0.0625f, 0.0625f);
            poseStack.pushPose();
            poseStack.translate(0.0f, -30.0f, -1.001f);
            poseStack.translate(0.0f, 2.75f, 0.0f);
            GraphicsHelper.drawTextFont(poseStack, bufferSource, this.dispatcher, cjkName, 0.0f, 0.0f, 0, 0, 0, packedLight, 0.25f, 0.25f, 0.25f);
            poseStack.translate(0.0f, 1.75f, 0.0f);
            GraphicsHelper.drawTextFont(poseStack, bufferSource, this.dispatcher, engName, 0.0f, 0.0f, 0, 0, 0, packedLight, 0.125f, 0.125f, 0.125f);
            UtilitiesClient.rotateYDegrees((PoseStack)poseStack, (float)180.0f);
            poseStack.translate(0.0f, -4.5f, -2.001f);
            poseStack.translate(0.0f, 2.75f, 0.0f);
            GraphicsHelper.drawTextFont(poseStack, bufferSource, this.dispatcher, cjkName, 0.0f, 0.0f, 0, 0, 0, packedLight, 0.25f, 0.25f, 0.25f);
            poseStack.translate(0.0f, 1.75f, 0.0f);
            GraphicsHelper.drawTextFont(poseStack, bufferSource, this.dispatcher, engName, 0.0f, 0.0f, 0, 0, 0, packedLight, 0.125f, 0.125f, 0.125f);
            poseStack.popPose();
        }
    }
}

