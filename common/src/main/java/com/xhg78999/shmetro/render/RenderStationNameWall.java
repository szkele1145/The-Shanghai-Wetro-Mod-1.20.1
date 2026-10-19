package com.xhg78999.shmetro.render;

import com.xhg78999.shmetro.blocks.BlockShanghaiStationNameOnWall;
import com.xhg78999.shmetro.entity.BlockEntityShanghaiStationNameOnWall;
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

public class RenderStationNameWall<T extends BlockEntityShanghaiStationNameOnWall>
extends BlockEntityRendererMapper<T>
implements BlockEntityRenderer<T> {
    private BlockEntityRenderDispatcher dispatcher;

    public RenderStationNameWall(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
        this.dispatcher = dispatcher;
    }

    public void render(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level world = entity.getLevel();
        if (world != null) {
            String eng;
            String cjk;
            BlockPos pos = entity.getBlockPos();
            Direction facing = (Direction)IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockShanghaiStationNameOnWall.FACING);
            Station station = RailwayData.getStation((Set)ClientData.STATIONS, (DataCache)ClientData.DATA_CACHE, (BlockPos)pos);
            String[] cjkAndEng;
            if (station == null) {
                cjkAndEng = StringUtil.getCjkAndEngMain(Text.translatable((String)"gui.mtr.untitled", (Object[])new Object[0]).toString());
            } else {
                cjkAndEng = StringUtil.getCjkAndEngMain(station.name);
            }
            cjk = cjkAndEng[0];
            eng = cjkAndEng[1];
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            UtilitiesClient.rotateZDegrees((PoseStack)poseStack, (float)180.0f);
            UtilitiesClient.rotateYDegrees((PoseStack)poseStack, (float)(-facing.toYRot()));
            poseStack.translate(0.0, 1.0, (double)-0.46f);
            GraphicsHelper.drawTextFont(poseStack, bufferSource, this.dispatcher, cjk, 0.0f, 0.0f, 0, 0, 0, packedLight, 0.03f, -0.03f, 0.03f, 1);
            GraphicsHelper.drawTextFont(poseStack, bufferSource, this.dispatcher, eng, 0.0f, -0.6f, 0, 0, 0, packedLight, 0.01f, -0.01f, 0.01f, 1);
            poseStack.popPose();
        }
    }
}

