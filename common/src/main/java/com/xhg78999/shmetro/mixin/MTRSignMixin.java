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
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(RenderRailwaySign.class)
public class MTRSignMixin {

    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "Lmtr/render/RenderRailwaySign$DrawTexture;drawTexture(Lnet/minecraft/resources/ResourceLocation;FFFZ)V"
            ),
            method = "drawSign(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lmtr/render/StoredMatrixTransformations;Lnet/minecraft/client/gui/Font;Lnet/minecraft/core/BlockPos;Ljava/lang/String;FFFFFLjava/util/Set;Lnet/minecraft/core/Direction;ILmtr/render/RenderRailwaySign$DrawTexture;)V",
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lmtr/client/ClientCache;requestStationIdToPlatforms(J)Ljava/util/Map;"),
                    to = @At(value = "INVOKE", target = "Lmtr/client/IDrawing;drawStringWithFont(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Ljava/lang/String;Lmtr/data/IGui$HorizontalAlignment;Lmtr/data/IGui$VerticalAlignment;FFFFFIZILmtr/client/IDrawing$DrawingCallback;)V")
            ),
            cancellable = true
    )
    private static void injectRenderingShanghaiSpecificSign(PoseStack matrices, MultiBufferSource vertexConsumers, StoredMatrixTransformations storedMatrixTransformations, Font textRenderer, BlockPos pos, String signId, float x, float y, float size, float maxWidthLeft, float maxWidthRight, Set<Long> selectedIds, Direction facing, int backgroundColor, RenderRailwaySign.DrawTexture drawTexture, CallbackInfo ci) {
        final CustomResources.CustomSign sign = RenderRailwaySign.getSign(signId);
        final float signSize = (sign.small ? 0.75F : 1.0F) * size;
        final float margin = (size - signSize) / 2;
        final boolean flipCustomText = sign.flipCustomText;
        final boolean flipTexture = sign.flipTexture;
        final MultiBufferSource.BufferSource immediate = RenderTrains.shouldNotRender(pos, RenderTrains.maxTrainRenderDistance / 2, (Direction) null) ? null : MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        if (signId.equals("mtr_custom_sign_shm_route")) {
            final Station station = RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, pos);
            if (station == null) {
                return;
            }

            final Map<Long, ?> platformPositions = ClientData.DATA_CACHE.requestStationIdToPlatforms(station.id);
            final Stream<Long> filtered = selectedIds.stream().filter(selectedId -> RailwayData.isBetween(selectedId, Long.MIN_VALUE, Long.MAX_VALUE)).map(Long::intValue).map(Long::valueOf).filter(platformPositions::containsKey);
            Objects.requireNonNull(platformPositions);

            final float start = Math.max(0, (flipCustomText ? maxWidthLeft : maxWidthRight) + size - margin * 2);
            final float height = size - margin * 2;
            float totalTextWidth = 0;

            final GraphicsCache.DynamicResource resourceLocation = ExtensionGraphics.getSHRoutes(filtered.collect(Collectors.toList()));
            final StoredMatrixTransformations storedMatrixTransformations2 = storedMatrixTransformations.copy();
            storedMatrixTransformations2.add(matricesNew -> matricesNew.translate(flipCustomText ? x + size - margin : x + margin, 0, 0));

            if (totalTextWidth > margin / 2) {
                totalTextWidth -= margin / 2;
            }
            if (totalTextWidth > start) {
                final float finalTotalTextWidth = totalTextWidth;
                storedMatrixTransformations2.add(matricesNew -> matricesNew.scale(start / finalTotalTextWidth, 1, 1));
            }

            final float xOffset = 0;
            final float width = height * (float) resourceLocation.width / (float) resourceLocation.height;
            final float finalXOffset = xOffset;
            final float finalWidth = width;

            RenderTrains.scheduleRender(resourceLocation.resourceLocation, true, RenderTrains.QueuedRenderLayer.LIGHT, (matricesNew, vertexConsumer) -> {
                storedMatrixTransformations2.transform(matricesNew);
                IDrawing.drawTexture(matricesNew, vertexConsumer, flipCustomText ? -finalXOffset - finalWidth : finalXOffset, margin, finalWidth, height, Direction.UP, 0xF000F0);
                matricesNew.popPose();
            });
            ci.cancel();
        }
    }
}
