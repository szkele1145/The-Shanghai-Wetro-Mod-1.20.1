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
        if (!signId.equals("mtr_custom_sign_shm_route") && !signId.equals("mtr_custom_sign_shm_route_flipped")) {
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
        // 【1.1.96 null/空守卫】getAllRoutesIncludingConnectingStations 在数据未就绪时可能返回 null;
        // selectedIds 理论上非 null, 但一并防御。此时没有可画的线路图, 直接取消渲染。
        if (routesInStation == null || routesInStation.isEmpty()) {
            ci.cancel();
            return;
        }
        final Set<Long> filtered = selectedIds == null ? java.util.Collections.emptySet() : selectedIds.stream()
                .filter(selectedId -> RailwayData.isBetween(selectedId, Long.MIN_VALUE, Long.MAX_VALUE))
                .map(Long::intValue)
                .filter(routesInStation::containsKey)
                .map(Integer::longValue)
                .collect(Collectors.toSet());

        // 【1.1.48 修单位混用】MTR 的 RenderRailwaySign.getMaxWidth 返回的是
        // **格数**(每格 +1, 有 custom text 的格 +0.5 —— 见其字节码: fconst_1/fadd
        // 与 fconst_2/fdiv), 不是像素。上游 1.19.4 的公式是
        //     start = Math.max(0, ((flip ? maxWidthLeft : maxWidthRight) + 1.0F) * size - margin * 2)
        // 即「(相邻格数 + 自身 1 格) * 每格尺寸 - 左右边距」, 是正确的格数→像素换算。
        // 移植时被误写成
        //     (flip ? maxWidthLeft : maxWidthRight) + 1.0F * size - margin * 2
        // —— 格数直接加像素, 单位混用: maxWidthLeft=2、size=32 时上游得 96,
        // 移植版得 34。该值经 finalWidth = Math.min(width, maxWidth) 压缩线路图
        // 贴图宽度 → 线路图被压到约 1 格宽, 严重过窄。
        final float maxWidth = Math.max(0.0F, ((flipCustomText ? maxWidthLeft : maxWidthRight) + 1.0F) * size - margin * 2);
        final float height = size - margin * 2;

        final GraphicsCache.DynamicResource resourceLocation = ExtensionGraphics.getSHRoutes(filtered.stream().collect(Collectors.toList()));
        if (resourceLocation == null) {
            ci.cancel();
            return;
        }

        final StoredMatrixTransformations storedMatrixTransformations2 = storedMatrixTransformations.copy();
        storedMatrixTransformations2.add(matricesNew -> matricesNew.translate(flipCustomText ? x + size - margin : x + margin, 0.0F, 0.0F));

        // 【1.1.48 等比缩放】原写法 finalWidth = Math.min(width, maxWidth) 只截宽度、
        // 高度不变 → 线路图被**非等比压扁**。改为等比: 宽度超限时按比例缩高度,
        // 两者都不超格子。drawTexture 的起点也随缩放后的实际宽度对齐,
        // 保证 flip 侧从 -finalWidth 起画、非 flip 侧从 0 起画时,
        // 贴图始终贴住本格内侧边缘。
        final float width = height * (float) resourceLocation.width / (float) resourceLocation.height;
        float drawWidth = width;
        float drawHeight = height;
        if (width > maxWidth && width > 0.0F) {
            final float scale = maxWidth / width;
            drawWidth = maxWidth;
            drawHeight = height * scale;
        }

        final float finalDrawWidth = drawWidth;
        final float finalDrawHeight = drawHeight;
        final float verticalOffset = (height - drawHeight) / 2.0F;
        RenderTrains.scheduleRender(resourceLocation.resourceLocation, true, RenderTrains.QueuedRenderLayer.LIGHT, (matricesNew, vertexConsumer) -> {
            storedMatrixTransformations2.transform(matricesNew);
            matricesNew.translate(0.0F, verticalOffset, 0.0F);
            IDrawing.drawTexture(matricesNew, vertexConsumer, flipCustomText ? -finalDrawWidth : 0.0F, margin, finalDrawWidth, finalDrawHeight, Direction.UP, 0xF000F0);
            matricesNew.popPose();
        });
        ci.cancel();
    }
}
