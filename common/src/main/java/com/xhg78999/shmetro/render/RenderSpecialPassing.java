package com.xhg78999.shmetro.render;

import com.xhg78999.shmetro.blocks.BlockPassingSM;
import com.xhg78999.shmetro.entity.BlockEntityPassing;
import com.xhg78999.shmetro.resource.BlockbenchModel;
import com.xhg78999.shmetro.resource.GraphicsCache;
import mtr.block.IBlock;
import mtr.data.TicketSystem;
import mtr.mappings.BlockEntityRendererMapper;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderTrains;
import mtr.render.StoredMatrixTransformations;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

public class RenderSpecialPassing
extends BlockEntityRendererMapper<BlockEntityPassing>
implements BlockEntityRenderer<BlockEntityPassing> {
    private final int type;
    private BlockbenchModel P_2000_OPEN;
    private BlockbenchModel P_2000_CLOSE;
    private BlockbenchModel P_2000_SIDE;
    private BlockbenchModel P_2010_OPEN;
    private BlockbenchModel P_2010_CLOSE;
    private BlockbenchModel P_2010_SIDE;
    private BlockbenchModel P_2020_OPEN;
    private BlockbenchModel P_2020_CLOSE;
    private BlockbenchModel P_2020_SIDE;

    public RenderSpecialPassing(BlockEntityRenderDispatcher dispatcher, int type) {
        super(dispatcher);
        this.type = type;
    }

    public void render(BlockEntityPassing entity, float tick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        Level world = entity.getLevel();
        if (world != null) {
            if (this.type == 0 && this.P_2000_OPEN == null) {
                this.P_2000_OPEN = GraphicsCache.CACHE.models.get("passing_2000_open");
                this.P_2000_CLOSE = GraphicsCache.CACHE.models.get("passing_2000_close");
                this.P_2000_SIDE = GraphicsCache.CACHE.models.get("passing_2000_side");
            }
            if (this.type == 2 && this.P_2010_OPEN == null) {
                this.P_2010_OPEN = GraphicsCache.CACHE.models.get("passing_2010_open");
                this.P_2010_CLOSE = GraphicsCache.CACHE.models.get("passing_2010_close");
                this.P_2010_SIDE = GraphicsCache.CACHE.models.get("passing_2010_side");
            }
            if (this.type == 3 && this.P_2020_OPEN == null) {
                this.P_2020_OPEN = GraphicsCache.CACHE.models.get("passing_2020_open");
                this.P_2020_CLOSE = GraphicsCache.CACHE.models.get("passing_2020_close");
                this.P_2020_SIDE = GraphicsCache.CACHE.models.get("passing_2020_side");
            }
            BlockPos pos = entity.getBlockPos();
            if (!(world.getBlockState(entity.getBlockPos()).getBlock() instanceof BlockPassingSM)) {
                return;
            }
            Direction facing = (Direction)IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockPassingSM.FACING);
            TicketSystem.EnumTicketBarrierOpen open = (TicketSystem.EnumTicketBarrierOpen)IBlock.getStatePropertySafe((BlockGetter)world, (BlockPos)pos, (Property)BlockPassingSM.OPEN);
            StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations();
            storedMatrixTransformations.add(matricesNew -> {
                matricesNew.translate(0.5 + (double)entity.getBlockPos().getX(), (double)entity.getBlockPos().getY(), 0.5 + (double)entity.getBlockPos().getZ());
                UtilitiesClient.rotateYDegrees((PoseStack)matricesNew, (float)(-facing.toYRot()));
                UtilitiesClient.rotateXDegrees((PoseStack)matricesNew, (float)180.0f);
            });
            switch (this.type) {
                case 0: {
                    Block neighborA = world.getBlockState(entity.getBlockPos().relative(facing.getOpposite(), 1)).getBlock();
                    if (neighborA instanceof BlockPassingSM) {
                        BlockPassingSM neighbour = (BlockPassingSM)neighborA;
                        if (neighbour.getPassingType() != 0 || ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() != neighbour.getEntrance()) {
                            RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", "textures/block/passing_2000_entrance_close.png"), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                                storedMatrixTransformations.transform(matricesNew);
                                this.P_2000_SIDE.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                                matricesNew.popPose();
                            });
                        }
                    } else {
                        RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", "textures/block/passing_2000_entrance_close.png"), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                            storedMatrixTransformations.transform(matricesNew);
                            this.P_2000_SIDE.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                            matricesNew.popPose();
                        });
                    }
                    if (open == TicketSystem.EnumTicketBarrierOpen.CLOSED) {
                        RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", String.format("textures/block/passing_2000_%s_close.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit")), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                            storedMatrixTransformations.transform(matricesNew);
                            this.P_2000_CLOSE.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                            matricesNew.popPose();
                        });
                        break;
                    }
                    RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", String.format("textures/block/passing_2000_%s_open.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit")), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                        storedMatrixTransformations.transform(matricesNew);
                        this.P_2000_OPEN.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                        matricesNew.popPose();
                    });
                    break;
                }
                case 1: {
                    throw new UnsupportedOperationException();
                }
                case 2: {
                    Block neighborB = world.getBlockState(entity.getBlockPos().relative(facing.getOpposite(), 1)).getBlock();
                    if (neighborB instanceof BlockPassingSM) {
                        BlockPassingSM neighbour = (BlockPassingSM)neighborB;
                        if (neighbour.getPassingType() != 2 || ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() != neighbour.getEntrance()) {
                            RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", "textures/block/passing_2010_entrance_close.png"), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                                storedMatrixTransformations.transform(matricesNew);
                                this.P_2010_SIDE.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                                matricesNew.popPose();
                            });
                        }
                    } else {
                        RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", "textures/block/passing_2010_entrance_close.png"), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                            storedMatrixTransformations.transform(matricesNew);
                            this.P_2010_SIDE.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                            matricesNew.popPose();
                        });
                    }
                    if (open == TicketSystem.EnumTicketBarrierOpen.CLOSED) {
                        RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", String.format("textures/block/passing_2010_%s_close.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit")), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                            storedMatrixTransformations.transform(matricesNew);
                            this.P_2010_CLOSE.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                            matricesNew.popPose();
                        });
                        break;
                    }
                    RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", String.format("textures/block/passing_2010_%s_open.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit")), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                        storedMatrixTransformations.transform(matricesNew);
                        this.P_2010_OPEN.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                        matricesNew.popPose();
                    });
                    break;
                }
                case 3: {
                    Block neighborC = world.getBlockState(entity.getBlockPos().relative(facing.getOpposite(), 1)).getBlock();
                    if (neighborC instanceof BlockPassingSM) {
                        BlockPassingSM neighbour = (BlockPassingSM)neighborC;
                        if (neighbour.getPassingType() != 3 || ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() != neighbour.getEntrance()) {
                            RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", "textures/block/passing_2020_entrance_close.png"), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                                storedMatrixTransformations.transform(matricesNew);
                                this.P_2020_SIDE.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                                matricesNew.popPose();
                            });
                        }
                    } else {
                        RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", "textures/block/passing_2020_entrance_close.png"), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                            storedMatrixTransformations.transform(matricesNew);
                            this.P_2020_SIDE.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                            matricesNew.popPose();
                        });
                    }
                    if (open == TicketSystem.EnumTicketBarrierOpen.CLOSED) {
                        RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", String.format("textures/block/passing_2020_%s_close.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit")), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                            storedMatrixTransformations.transform(matricesNew);
                            this.P_2020_CLOSE.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                            matricesNew.popPose();
                        });
                        break;
                    }
                    RenderTrains.scheduleRender((ResourceLocation)new ResourceLocation("shmetro", String.format("textures/block/passing_2020_%s_open.png", ((BlockPassingSM)world.getBlockState(entity.getBlockPos()).getBlock()).getEntrance() ? "entrance" : "exit")), (boolean)false, (RenderTrains.QueuedRenderLayer)RenderTrains.QueuedRenderLayer.EXTERIOR, (matricesNew, vertexConsumer) -> {
                        storedMatrixTransformations.transform(matricesNew);
                        this.P_2020_OPEN.renderToBuffer((PoseStack)matricesNew, (VertexConsumer)vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
                        matricesNew.popPose();
                    });
                    break;
                }
            }
        }
    }
}

