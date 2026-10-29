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
    // 热路径缓存: 4 种组合 (type 1/2 x side left/right)
    private static final net.minecraft.resources.ResourceLocation[][] RAILING_TEX = new net.minecraft.resources.ResourceLocation[2][2];
    static {
        String[] types = { "1", "2" };
        String[] sides = { "left", "right" };
        for (int t = 0; t < 2; t++) {
            for (int s = 0; s < 2; s++) {
                RAILING_TEX[t][s] = new net.minecraft.resources.ResourceLocation(String.format("shmetro:textures/block/erailing_%s_%s.png", types[t], sides[s]));
            }
        }
    }

    private final int type;
    // 模型引用缓存: 原实现在 render() 里每帧都 models.get("erailing_door"),
    // 既是多余的哈希查找, 也在模型缺失时每帧解引用 null -> NPE 崩溃客户端。
    private EntityModel model;

    public RenderERailingDoor(BlockEntityRenderDispatcher dispatcher, int type) {
        super(dispatcher, type);
        this.type = type;
    }

    public void render(BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        Level world = entity.getLevel();
        if (world != null) {
            if (this.model == null) {
                this.model = (EntityModel)GraphicsCache.CACHE.models.get("erailing_door");
            }
            // 模型缺失(资源包损坏 / 该模型被移除)时直接跳过渲染, 避免 NPE。
            if (this.model == null) {
                return;
            }
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
                // 贴图索引: type 1 -> erailing_1_*, type 2 -> erailing_2_*。
                // 原实现用魔数 114514 判 type==1, 而 MainClient 注册时传的
                // 也是 114514/191981 —— 两处自洽但都是上游残留占位魔数,
                // 与 BlockRailingDoor.type(1/2) 完全对不上。一旦有人把注册值
                // 改成 1/2 对齐, 这里会静默选错贴图。此处一并统一。
                RenderTrains.scheduleRender((ResourceLocation)RAILING_TEX[this.type == 1 ? 0 : 1][side ? 1 : 0], (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                    storedMatrixTransformations.transform(matricesNew);
                    this.model.renderToBuffer(matricesNew, vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                    matricesNew.popPose();
                });
            }
        }
    }
}

