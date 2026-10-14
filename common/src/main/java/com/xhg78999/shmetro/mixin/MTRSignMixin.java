package com.xhg78999.shmetro.mixin;

import com.xhg78999.shmetro.resource.ExtensionGraphics;
import com.xhg78999.shmetro.resource.GraphicsCache;
import mtr.client.ClientData;
import mtr.client.CustomResources;
import mtr.client.IDrawing;
import mtr.data.RailwayData;
import mtr.data.Station;
import mtr.render.RenderRailwaySign;
import mtr.render.RenderTrains;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MTR 1.20.1 (3.2.2) 适配版：
 * 原 1.19.4 mixin 依赖的 slice 锚点（ClientCache.requestStationIdToPlatforms /
 * IDrawing.drawStringWithFont）在 MTR 3.2.2 的 drawSign 中已不存在，
 * 改为 HEAD 注入：当 signId 为 shm_route 自定义线路图时整体接管渲染。
 */
@Mixin(RenderRailwaySign.class)
public class MTRSignMixin {

    @Inject(
            at = @At("HEAD"),
            method = "drawSign(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lmtr/render/StoredMatrixTransformations;Lnet/minecraft/client/gui/Font;Lnet/minecraft/core/BlockPos;Ljava/lang/String;FFFFFLjava/util/Set;Lnet/minecraft/core/Direction;ILmtr/render/RenderRailwaySign$DrawTexture;)V",
            cancellable = true
    )
    private static void injectRenderingShanghaiSpecificSign(PoseStack matrices, MultiBufferSource vertexConsumers, StoredMatrixTransformations storedMatrixTransformations, Font textRenderer, BlockPos pos, String signId, float x, float y, float size, float maxWidthLeft, float maxWidthRight, Set<Long> selectedIds, Direction facing, int backgroundColor, RenderRailwaySign.DrawTexture drawTexture, CallbackInfo ci) {
        if (!signId.equals("mtr_custom_sign_shm_route")) {
            return;
        }

        final CustomResources.CustomSign sign = RenderRailwaySign.getSign(signId);
        if (sign == null) {
            return;
        }

        final float signSize = (sign.small ? 0.75F : 1.0F) * size;
        final float margin = (size - signSize) / 2;
        final boolean flipCustomText = sign.flipCustomText;
        final MultiBufferSource.BufferSource immediate = RenderTrains.shouldNotRender(pos, RenderTrains.maxTrainRenderDistance / 2, null) ? null : MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        final Station station = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, pos);
        if (station == null) {
            ci.cancel();
            return;
        }

        final Map<Integer, ?> routesInStation = ClientData.DATA_CACHE.getAllRoutesIncludingConnectingStations(station);
        final Set<Long> filtered = selectedIds.stream()
                .filter(selectedId -> RailwayData.isBetween(selectedId, Long.MIN_VALUE, Long.MAX_VALUE))
                .map(Long::intValue)
                .filter(routesInStation::containsKey)
                .map(Integer::longValue)
                .collect(Collectors.toSet());

        final float maxWidth = Math.max(0.0F, (flipCustomText ? maxWidthLeft : maxWidthRight) + 1.0F * size - margin * 2);
        final float height = size - margin * 2;

        final GraphicsCache.DynamicResource resourceLocation = ExtensionGraphics.getSHRoutes(filtered.stream().collect(Collectors.toList()));
        if (resourceLocation == null) {
            ci.cancel();
            return;
        }

        final StoredMatrixTransformations storedMatrixTransformations2 = storedMatrixTransformations.copy();
        storedMatrixTransformations2.add(matricesNew -> matricesNew.translate(flipCustomText ? x + size - margin : x + margin, 0.0F, 0.0F));

        final float width = height * (float) resourceLocation.width / (float) resourceLocation.height;
        final float finalWidth = Math.min(width, maxWidth);

        RenderTrains.scheduleRender(resourceLocation.resourceLocation, true, RenderTrains.QueuedRenderLayer.LIGHT, (matricesNew, vertexConsumer) -> {
            storedMatrixTransformations2.transform(matricesNew);
            IDrawing.drawTexture(matricesNew, vertexConsumer, flipCustomText ? -finalWidth : 0.0F, margin, finalWidth, height, Direction.UP, 0xF000F0);
            matricesNew.popPose();
        });
        ci.cancel();
    }
}
