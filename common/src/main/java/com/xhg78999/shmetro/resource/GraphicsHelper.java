package com.xhg78999.shmetro.resource;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.imageio.ImageIO;
import mtr.data.IGui;
import mtr.mappings.Text;
import mtr.mappings.Utilities;
import mtr.mappings.UtilitiesClient;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import org.joml.Matrix4f;

public class GraphicsHelper {
    public static void drawText(Style style, PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, float sx, float sy, float sz, int maxwidth) {
        FormattedCharSequence orderedText = Text.literal((String)text).withStyle(style).getVisualOrderText();
        int totalWidth = (int)Math.ceil(Minecraft.getInstance().font.width(orderedText));
        float offset = IGui.VerticalAlignment.CENTER.getOffset(y, 10.0f);
        float xOffset = IGui.HorizontalAlignment.CENTER.getOffset(IGui.HorizontalAlignment.CENTER.getOffset(x, (float)totalWidth), 0.0f);
        float ratio = 1.0f;
        if (maxwidth != 0 && totalWidth > maxwidth) {
            ratio = maxwidth / totalWidth;
        }
        poseStack.scale(sx * ratio, sy * ratio, sz);
        UtilitiesClient.drawInBatch((net.minecraft.client.gui.Font) Minecraft.getInstance().font, (FormattedCharSequence)orderedText, (float)xOffset, (float)offset, (int)((r << 16) + (g << 8) + b), (boolean)false, (Matrix4f)poseStack.last().pose(), (MultiBufferSource)buffer, (int)0, (int)light);
        poseStack.scale(1.0f / sx / ratio, 1.0f / sy / ratio, 1.0f / sz);
    }

    public static void drawText(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, float sx, float sy, float sz) {
        GraphicsHelper.drawText(Style.EMPTY, poseStack, buffer, dispatcher, text, x, y, r, g, b, light, sx, sy, sz, 0);
    }

    public static void drawText(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light) {
        GraphicsHelper.drawText(Style.EMPTY, poseStack, buffer, dispatcher, text, x, y, r, g, b, light, 1.0f, 1.0f, 1.0f, 0);
    }

    public static void drawTextFont(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light) {
        GraphicsHelper.drawText(Style.EMPTY.withFont(new ResourceLocation("shmetro", "shmetro")), poseStack, buffer, dispatcher, text, x, y, r, g, b, light, 1.0f, 1.0f, 1.0f, 0);
    }

    public static void drawTextFont(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, float sx, float sy, float sz) {
        GraphicsHelper.drawText(Style.EMPTY.withFont(new ResourceLocation("shmetro", "shmetro")), poseStack, buffer, dispatcher, text, x, y, r, g, b, light, sx * 0.7f, sy * 0.7f, sz * 0.7f, 0);
    }

    public static void drawText(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, float sx, float sy, float sz, int maxwidth) {
        GraphicsHelper.drawText(Style.EMPTY, poseStack, buffer, dispatcher, text, x, y, r, g, b, light, sx, sy, sz, maxwidth);
    }

    public static void drawText(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, int maxwidth) {
        GraphicsHelper.drawText(Style.EMPTY, poseStack, buffer, dispatcher, text, x, y, r, g, b, light, 1.0f, 1.0f, 1.0f, maxwidth);
    }

    public static void drawTextFont(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, int maxwidth) {
        GraphicsHelper.drawText(Style.EMPTY.withFont(new ResourceLocation("shmetro", "shmetro")), poseStack, buffer, dispatcher, text, x, y, r, g, b, light, 1.0f, 1.0f, 1.0f, maxwidth);
    }

    public static void drawTextFont(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, float sx, float sy, float sz, int maxwidth) {
        GraphicsHelper.drawText(Style.EMPTY.withFont(new ResourceLocation("shmetro", "shmetro")), poseStack, buffer, dispatcher, text, x, y, r, g, b, light, sx * 0.7f, sy * 0.7f, sz * 0.7f, maxwidth);
    }

    public static void pasteImage(Graphics2D gph, ResourceLocation res, int x, int y) {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        try {
            BufferedImage bfi = ImageIO.read(Utilities.getInputStream((Optional)rm.getResource(res)));
            gph.drawImage((Image)bfi, x, y, null);
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
        }
    }

    public static void drawImageTextAlign(Graphics2D gph, Font font, String text, int x, int y, String alignment) {
        float length = GraphicsHelper.measureImageText(gph, font, text);
        int offset = switch (alignment) {
            case "left" -> x;
            case "right" -> (int)((float)x - length);
            case "middle" -> (int)((float)x - length / 2.0f);
            default -> throw new NoSuchElementException("Incorrect alignment");
        };
        gph.setFont(font);
        gph.drawString(text, offset, y);
    }

    public static int measureImageText(Graphics2D gph, Font font, String text) {
        return gph.getFontMetrics(font).stringWidth(text);
    }

    public static Color foreground(int color) {
        Color c = new Color(color);
        return (double)c.getRed() * 0.299 + (double)c.getGreen() * 0.587 + (double)c.getBlue() * 0.514 < 0.5 ? Color.BLACK : Color.WHITE;
    }
}

