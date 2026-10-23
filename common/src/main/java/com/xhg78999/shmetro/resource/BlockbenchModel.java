package com.xhg78999.shmetro.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import mtr.mappings.ModelDataWrapper;
import mtr.mappings.ModelMapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.model.Model;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;

public class BlockbenchModel
extends EntityModel<Entity> {
    public final List<ModelMapper> parts = new ArrayList<ModelMapper>();
    public ModelMapper bone;

    public BlockbenchModel(String model) {
        this.parseBBModel(model);
    }

    public void parseBBModel(String raw) {
        JsonObject model = (JsonObject)JsonParser.parseString((String)raw);
        try {
            JsonObject resolution = model.has("resolution") ? model.getAsJsonObject("resolution") : null;
            int textureWidth = (resolution != null && resolution.has("width")) ? resolution.get("width").getAsInt() : 16;
            int textureHeight = (resolution != null && resolution.has("height")) ? resolution.get("height").getAsInt() : 16;
            ModelDataWrapper modelDataWrapper = new ModelDataWrapper((Model)this, textureWidth, textureHeight);
            this.bone = new ModelMapper(modelDataWrapper);
            model.getAsJsonArray("elements").forEach(element -> {
                JsonObject elementObject = element.getAsJsonObject();
                ModelMapper child = new ModelMapper(modelDataWrapper);
                Double[] origin = new Double[]{0.0, 0.0, 0.0};
                BlockbenchModel.getArrayFromValue(origin, elementObject, "origin", JsonElement::getAsDouble);
                Double[] rotation = new Double[]{0.0, 0.0, 0.0};
                BlockbenchModel.getArrayFromValue(rotation, elementObject, "rotation", JsonElement::getAsDouble);
                Integer[] uvOffset = new Integer[]{0, 0};
                BlockbenchModel.getArrayFromValue(uvOffset, elementObject, "uv_offset", JsonElement::getAsInt);
                Double[] posFrom = new Double[]{0.0, 0.0, 0.0};
                BlockbenchModel.getArrayFromValue(posFrom, elementObject, "from", JsonElement::getAsDouble);
                Double[] posTo = new Double[]{0.0, 0.0, 0.0};
                BlockbenchModel.getArrayFromValue(posTo, elementObject, "to", JsonElement::getAsDouble);
                double inflate = elementObject.has("inflate") ? elementObject.get("inflate").getAsDouble() : 0.0;
                boolean mirror = elementObject.has("shade") && !elementObject.get("shade").getAsBoolean();
                child.setPos(-origin[0].floatValue(), -origin[1].floatValue(), origin[2].floatValue());
                this.bone.addChild(child);
                child.setRotationAngle(-((float)Math.toRadians(rotation[0])), -((float)Math.toRadians(rotation[1])), (float)Math.toRadians(rotation[2]));
                child.texOffs(uvOffset[0].intValue(), uvOffset[1].intValue()).addBox(origin[0].floatValue() - posTo[0].floatValue(), origin[1].floatValue() - posTo[1].floatValue(), posFrom[2].floatValue() - origin[2].floatValue(), Math.round(posTo[0].floatValue() - posFrom[0].floatValue()), Math.round(posTo[1].floatValue() - posFrom[1].floatValue()), Math.round(posTo[2].floatValue() - posFrom[2].floatValue()), (float)inflate, mirror);
            });
            modelDataWrapper.setModelPart(textureWidth, textureHeight);
            this.bone.setModelPart();
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
        }
    }

    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.bone.render(poseStack, buffer, 0.0f, 0.0f, 0.0f, 0.0f, packedLight, packedOverlay);
    }

    private static <T> void getArrayFromValue(T[] array, JsonObject jsonObject, String key, Function<JsonElement, T> function) {
        if (jsonObject.has(key)) {
            JsonArray jsonArray = jsonObject.getAsJsonArray(key);
            // 数组长度不足时直接忽略该字段: 原实现 jsonArray.get(i) 会抛
            // IndexOutOfBoundsException, 该异常会中断 elements 的 forEach,
            // 导致该 bbmodel 后续所有元素丢失(整个模型渲染不完整)。
            int n = Math.min(array.length, jsonArray.size());
            for (int i = 0; i < n; ++i) {
                array[i] = function.apply(jsonArray.get(i));
            }
        }
    }
}

