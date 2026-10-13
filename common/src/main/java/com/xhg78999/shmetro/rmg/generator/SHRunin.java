package com.xhg78999.shmetro.rmg.generator;

import com.xhg78999.shmetro.resource.GraphicsCache;
import com.xhg78999.shmetro.resource.GraphicsHelper;
import com.xhg78999.shmetro.rmg.RMGMain;
import com.xhg78999.shmetro.utils.StringUtil;
import java.awt.Color;
import java.awt.Font;
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
import mtr.data.Route;
import mtr.data.Station;
import com.mojang.blaze3d.platform.NativeImage;

public class SHRunin {
    public static NativeImage generate(boolean direction, Station next, Station prev, Station now, Route line, boolean flip) throws IOException {
        ByteArrayInputStream ist;
        int width = 1816;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, 2);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (line.circularState != Route.CircularState.NONE) {
            if (prev == null) {
                prev = (Station)ClientData.DATA_CACHE.platformIdToStation.get(((Route.RoutePlatform)line.platformIds.get((int)(line.platformIds.size() - 2))).platformId);
            }
            if (next == null) {
                next = (Station)ClientData.DATA_CACHE.platformIdToStation.get(((Route.RoutePlatform)line.platformIds.get((int)1)).platformId);
            }
        }
        SHRunin.drawGeneralStation(g2d, direction, next, prev, now, line, flip);
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

    private static void drawGeneralStation(Graphics2D g2d, boolean direction, Station next, Station prev, Station now, Route line, boolean flip) {
        g2d.setColor(new Color(line.color));
        if (direction) {
            g2d.fillRect(18, 353, 890, 12);
            g2d.fillPolygon(new int[]{6, 18, 18}, new int[]{365, 365, 353}, 3);
        } else {
            g2d.fillRect(908, 353, 890, 12);
            g2d.fillPolygon(new int[]{1798, 1798, 1810}, new int[]{353, 365, 365}, 3);
        }
        g2d.setColor(Color.GRAY);
        if (direction) {
            g2d.fillRect(908, 353, 902, 12);
        } else {
            g2d.fillRect(6, 353, 902, 12);
        }
        SHRunin.drawCurrentText(g2d, direction, next, prev, now, line);
        SHRunin.drawPrevStn(g2d, direction, next, prev, now, line, flip);
        SHRunin.drawNextStn(g2d, direction, next, prev, now, line, flip);
    }

    private static void drawCurrentText(Graphics2D g2d, boolean direction, Station next, Station prev, Station now, Route line) {
        Font fontCjk = GraphicsCache.CACHE.fontCjk.deriveFont(112.0f);
        Font fontEng = GraphicsCache.CACHE.font.deriveFont(36.0f);
        g2d.setColor(Color.BLACK);
        GraphicsHelper.drawImageTextAlign(g2d, fontCjk, StringUtil.getCjkMain(RMGMain.getStationName(now)), 908, 250, "middle");
        GraphicsHelper.drawImageTextAlign(g2d, fontEng, StringUtil.getEngMain(RMGMain.getStationName(now)), 908, 300, "middle");
    }

    private static void drawPrevStn(Graphics2D g2d, boolean direction, Station next, Station prev, Station now, Route line, boolean flip) {
        if ((flip ? prev : next) == null) {
            return;
        }
        Font fontCjk = GraphicsCache.CACHE.fontCjk.deriveFont(48.0f);
        Font fontEng = GraphicsCache.CACHE.font.deriveFont(24.0f);
        Font fontCjkSmall = GraphicsCache.CACHE.fontCjk.deriveFont(22.0f);
        Font fontEngSmall = GraphicsCache.CACHE.font.deriveFont(12.0f);
        g2d.setColor(Color.GRAY);
        if (direction) {
            GraphicsHelper.drawImageTextAlign(g2d, fontCjkSmall, "\u4e0a\u4e00\u7ad9", 1786, 215, "right");
            GraphicsHelper.drawImageTextAlign(g2d, fontEngSmall, "Past stop", 1716, 215, "right");
            GraphicsHelper.drawImageTextAlign(g2d, fontCjk, StringUtil.getCjkMain(RMGMain.getStationName(flip ? next : prev)), 1786, 273, "right");
            GraphicsHelper.drawImageTextAlign(g2d, fontEng, StringUtil.getEngMain(RMGMain.getStationName(flip ? next : prev)), 1786, 301, "right");
        } else {
            GraphicsHelper.drawImageTextAlign(g2d, fontCjkSmall, "\u4e0a\u4e00\u7ad9", 30, 215, "left");
            GraphicsHelper.drawImageTextAlign(g2d, fontEngSmall, "Past stop", 100, 215, "left");
            GraphicsHelper.drawImageTextAlign(g2d, fontCjk, StringUtil.getCjkMain(RMGMain.getStationName(flip ? prev : next)), 30, 273, "left");
            GraphicsHelper.drawImageTextAlign(g2d, fontEng, StringUtil.getEngMain(RMGMain.getStationName(flip ? prev : next)), 30, 301, "left");
        }
    }

    private static void drawNextStn(Graphics2D g2d, boolean direction, Station next, Station prev, Station now, Route line, boolean flip) {
        if ((flip ? next : prev) == null) {
            return;
        }
        Font fontCjk = GraphicsCache.CACHE.fontCjk.deriveFont(48.0f);
        Font fontEng = GraphicsCache.CACHE.font.deriveFont(24.0f);
        Font fontCjkSmall = GraphicsCache.CACHE.fontCjk.deriveFont(22.0f);
        Font fontEngSmall = GraphicsCache.CACHE.font.deriveFont(12.0f);
        g2d.setColor(Color.BLACK);
        if (direction) {
            GraphicsHelper.drawImageTextAlign(g2d, fontCjkSmall, "\u4e0b\u4e00\u7ad9", 30, 215, "left");
            GraphicsHelper.drawImageTextAlign(g2d, fontEngSmall, "Next stop", 100, 215, "left");
            GraphicsHelper.drawImageTextAlign(g2d, fontCjk, StringUtil.getCjkMain(RMGMain.getStationName(flip ? prev : next)), 30, 273, "left");
            GraphicsHelper.drawImageTextAlign(g2d, fontEng, StringUtil.getEngMain(RMGMain.getStationName(flip ? prev : next)), 30, 301, "left");
        } else {
            GraphicsHelper.drawImageTextAlign(g2d, fontCjkSmall, "\u4e0b\u4e00\u7ad9", 1786, 215, "right");
            GraphicsHelper.drawImageTextAlign(g2d, fontEngSmall, "Next stop", 1716, 215, "right");
            GraphicsHelper.drawImageTextAlign(g2d, fontCjk, StringUtil.getCjkMain(RMGMain.getStationName(flip ? next : prev)), 1786, 273, "right");
            GraphicsHelper.drawImageTextAlign(g2d, fontEng, StringUtil.getEngMain(RMGMain.getStationName(flip ? next : prev)), 1786, 301, "right");
        }
    }
}

