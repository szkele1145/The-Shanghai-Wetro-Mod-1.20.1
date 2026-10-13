package com.xhg78999.shmetro.rmg.generator;

import com.xhg78999.shmetro.resource.GraphicsCache;
import com.xhg78999.shmetro.resource.GraphicsHelper;
import com.xhg78999.shmetro.rmg.RMGMain;
import com.xhg78999.shmetro.utils.StringUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.data.Station;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;

public class SHDestination {
    public static NativeImage generate(boolean direction, boolean flip, Route line, int width, Platform platform) throws IOException {
        ByteArrayInputStream ist;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, 2);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        SHDestination.draw(g2d, direction, line, flip, width, platform);
        g2d.dispose();
        NativeImage nat = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write((RenderedImage)image, "png", os);
            ist = new ByteArrayInputStream(os.toByteArray());
        }
        catch (IOException e) {
            e.printStackTrace();
            return nat;
        }
        return NativeImage.read((InputStream) ist);
    }

    private static void draw(Graphics2D g2d, boolean direction, Route line, boolean flip, int width, Platform platform) {
        g2d.setColor(new Color(line.color));
        if (direction) {
            g2d.fillRect(162, 353, width - 312, 12);
            g2d.fillPolygon(new int[]{150, 162, 162}, new int[]{365, 365, 353}, 3);
            GraphicsHelper.pasteImage(g2d, new ResourceLocation("shmetro", "textures/rendering/arrow_left.png"), 162, 223);
            g2d.setColor(Color.BLACK);
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(70.0f), String.format("\u5f80%s", StringUtil.getCjkMain(((Station)ClientData.DATA_CACHE.platformIdToStation.get((Object)Long.valueOf((long)(flip ? line.getFirstPlatformId() : line.getLastPlatformId())))).name)), 313, 277, "left");
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(25.0f), String.format("To %s", StringUtil.getEngMain(((Station)ClientData.DATA_CACHE.platformIdToStation.get((Object)Long.valueOf((long)(flip ? line.getFirstPlatformId() : line.getLastPlatformId())))).name)), 313, 310, "left");
            SHDestination.drawLineBig(g2d, line, width, false);
        } else {
            g2d.fillRect(150, 353, width - 312, 12);
            g2d.fillPolygon(new int[]{width - 162, width - 162, width - 150}, new int[]{353, 365, 365}, 3);
            GraphicsHelper.pasteImage(g2d, new ResourceLocation("shmetro", "textures/rendering/arrow_right.png"), width - 289, 223);
            g2d.setColor(Color.BLACK);
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(70.0f), String.format("\u5f80%s", StringUtil.getCjkMain(((Station)ClientData.DATA_CACHE.platformIdToStation.get((Object)Long.valueOf((long)(!flip ? line.getFirstPlatformId() : line.getLastPlatformId())))).name)), width - 184 - 129, 277, "right");
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(25.0f), String.format("To %s", StringUtil.getEngMain(((Station)ClientData.DATA_CACHE.platformIdToStation.get((Object)Long.valueOf((long)(!flip ? line.getFirstPlatformId() : line.getLastPlatformId())))).name)), width - 184 - 129, 310, "right");
            SHDestination.drawLineBig(g2d, line, 150, true);
        }
        SHDestination.drawPlatformId(g2d, platform, width / 2);
    }

    private static void drawLineBig(Graphics2D g2d, Route line, int x, boolean left) {
        g2d.setColor(new Color(line.color));
        if (RMGMain.isDigitLine(line.name)) {
            int size = 262;
            if (left) {
                g2d.translate(150, 190);
            } else {
                g2d.translate(x - 150 - size, 190);
            }
            g2d.fillRect(0, 0, 108, 120);
            g2d.setColor(GraphicsHelper.foreground(line.color));
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(96.0f), RMGMain.getDigitLine(line.name), 54, 100, "middle");
            g2d.setColor(Color.BLACK);
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(68.0f), "\u53f7\u7ebf", 120, 68, "left");
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(30.0f), StringUtil.getEngMain(line.name), 120, 110, "left");
            if (left) {
                g2d.translate(-150, -190);
            } else {
                g2d.translate(-x + 150 + size, -190);
            }
        } else {
            int size = Math.max(GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(68.0f), StringUtil.getCjkMain(line.name)), GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(30.0f), StringUtil.getEngMain(line.name))) + 10;
            if (left) {
                g2d.translate(150, 190);
            } else {
                g2d.translate(x - 150 - size, 190);
            }
            g2d.fillRect(0, 0, size, 120);
            g2d.setColor(GraphicsHelper.foreground(line.color));
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(68.0f), StringUtil.getCjkMain(line.name), size / 2, 68, "middle");
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(30.0f), StringUtil.getEngMain(line.name), size / 2, 110, "middle");
            if (left) {
                g2d.translate(-150, -190);
            } else {
                g2d.translate(-x + 150 + size, -190);
            }
        }
    }

    private static void drawPlatformId(Graphics2D g2d, Platform platform, int x) {
        g2d.translate(x - 102, 180);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawOval(-60, 20, 120, 120);
        GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(100.0f), "\u7ad9\u53f0", 67, 120, "left");
        GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(120.0f), platform.name, 0, 130, "middle");
        g2d.translate(102 - x, -180);
    }
}

