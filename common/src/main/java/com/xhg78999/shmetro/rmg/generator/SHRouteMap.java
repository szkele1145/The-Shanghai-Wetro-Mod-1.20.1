package com.xhg78999.shmetro.rmg.generator;

import com.xhg78999.shmetro.resource.GraphicsCache;
import com.xhg78999.shmetro.resource.GraphicsHelper;
import com.xhg78999.shmetro.rmg.RMGMain;
import com.xhg78999.shmetro.utils.StringUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import mtr.client.ClientCache;
import mtr.data.Route;
import mtr.data.Station;
import com.mojang.blaze3d.platform.NativeImage;

public class SHRouteMap {
    public static NativeImage generate(boolean direction, boolean flip, Station now, Route line, int width, int nowpos) throws IOException {
        ByteArrayInputStream ist;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, 2);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        SHRouteMap.drawMap(g2d, direction, flip, now, line, width, nowpos);
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

    private static void drawMap(Graphics2D g2d, boolean direction, boolean flip, Station now, Route line, int width, int nowpos) {
        g2d.setColor(new Color(line.color));
        if (direction) {
            g2d.fillRect(162, 353, width - 312, 12);
            g2d.fillPolygon(new int[]{150, 162, 162}, new int[]{365, 365, 353}, 3);
        } else {
            g2d.fillRect(150, 353, width - 312, 12);
            g2d.fillPolygon(new int[]{width - 162, width - 162, width - 150}, new int[]{353, 365, 365}, 3);
        }
        SHRouteMap.drawStationBricks(g2d, direction, flip, now, line, width, nowpos);
    }

    private static void drawStationBricks(Graphics2D g2d, boolean direction, boolean flip, Station now, Route line, int width, int nowpos) {
        int basic = direction ? 188 : 176;
        List pids = line.platformIds.stream().collect(Collectors.toList());
        if (!flip) {
            Collections.reverse(pids);
            nowpos = pids.size() - nowpos - 1;
        }
        if (direction) {
            for (int i = 0; i < pids.size(); ++i) {
                if (i == nowpos) {
                    g2d.setColor(Color.RED);
                    GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(17.0f), "\u672c\u7ad9", basic, 383, "middle");
                    g2d.setColor(Color.GRAY);
                    g2d.fillRect(basic, 353, width - basic - 150, 12);
                }
                if (i <= nowpos) {
                    g2d.setColor(new Color(line.color));
                } else {
                    g2d.setColor(Color.GRAY);
                }
                g2d.fillRect(basic - 6, 341, 12, 24);
                SHRouteMap.drawStation(g2d, direction, RMGMain.getStationFromPlatform(((Route.RoutePlatform)pids.get((int)i)).platformId), line, basic - 6, i == nowpos, i > nowpos);
                basic += SHRouteMap.calculateInterval(pids.size(), width);
            }
        } else {
            for (int i = 0; i < pids.size(); ++i) {
                if (i == nowpos) {
                    g2d.setColor(Color.RED);
                    GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(17.0f), "\u672c\u7ad9", basic, 383, "middle");
                    g2d.setColor(Color.GRAY);
                    g2d.fillRect(150, 353, basic - 150, 12);
                }
                if (i >= nowpos) {
                    g2d.setColor(new Color(line.color));
                } else {
                    g2d.setColor(Color.GRAY);
                }
                g2d.fillRect(basic - 6, 341, 12, 24);
                SHRouteMap.drawStation(g2d, direction, RMGMain.getStationFromPlatform(((Route.RoutePlatform)pids.get((int)i)).platformId), line, basic + 6, i == nowpos, i < nowpos);
                if (i == nowpos) {
                    g2d.setColor(Color.RED);
                    GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(17.0f), "\u672c\u7ad9", basic, 383, "middle");
                }
                basic += SHRouteMap.calculateInterval(pids.size(), width);
            }
        }
    }

    private static void drawStation(Graphics2D g2d, boolean direction, Station now, Route line, int baselineStn, boolean isNow, boolean isPassed) {
        BufferedImage image = new BufferedImage(353, 28, 2);
        Graphics2D g2da = image.createGraphics();
        g2da.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2da.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        List<ClientCache.ColorNameTuple> interchanges = RMGMain.getInterchangeRoutes(now.id, line);
        if (isPassed) {
            g2da.setColor(Color.GRAY);
        } else {
            g2da.setColor(Color.BLACK);
        }
        if (isNow) {
            g2da.setColor(Color.RED);
        }
        if (direction) {
            GraphicsHelper.drawImageTextAlign(g2da, GraphicsCache.CACHE.fontCjk.deriveFont(16.0f), StringUtil.getCjkMain(now.name), 6, 16, "left");
            GraphicsHelper.drawImageTextAlign(g2da, GraphicsCache.CACHE.font.deriveFont(8.0f), StringUtil.getEngMain(now.name), 6, 26, "left");
        } else {
            GraphicsHelper.drawImageTextAlign(g2da, GraphicsCache.CACHE.fontCjk.deriveFont(16.0f), StringUtil.getCjkMain(now.name), 347, 16, "right");
            GraphicsHelper.drawImageTextAlign(g2da, GraphicsCache.CACHE.font.deriveFont(8.0f), StringUtil.getEngMain(now.name), 347, 26, "right");
        }
        if (!interchanges.isEmpty()) {
            if (isPassed) {
                g2da.setColor(Color.GRAY);
                g2da.drawLine(0, 27, 353, 28);
            } else {
                g2da.setColor(Color.BLACK);
                g2da.drawLine(0, 27, 353, 28);
            }
            SHRouteMap.drawTransferBricks(g2da, direction, interchanges);
        }
        g2da.dispose();
        if (direction) {
            g2d.translate(baselineStn - 6, 321);
            g2d.rotate(-45.0);
            g2d.drawImage((Image)image, 0, 0, null);
            g2d.rotate(45.0);
            g2d.translate(-baselineStn + 6, -321);
        } else {
            g2d.translate(baselineStn - 162, 31);
            g2d.rotate(45.0);
            g2d.drawImage((Image)image, 0, 0, null);
            g2d.rotate(-45.0);
            g2d.translate(-baselineStn + 162, -31);
        }
    }

    private static void drawTransferBricks(Graphics2D g2d, boolean direction, List<ClientCache.ColorNameTuple> interchanges) {
        if (interchanges.isEmpty()) {
            return;
        }
        if (direction) {
            int offset = 353;
            for (int i = interchanges.size() - 1; i >= 0; --i) {
                ClientCache.ColorNameTuple line = interchanges.get(i);
                if (line == null) continue;
                if (RMGMain.isDigitLine(line.name)) {
                    g2d.setColor(new Color(line.color));
                    g2d.fillRect(offset - 20, 7, 20, 21);
                    g2d.setColor(GraphicsHelper.foreground(line.color));
                    GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(14.0f), RMGMain.getDigitLine(line.name), offset - 10, 24, "middle");
                    offset -= 25;
                    continue;
                }
                int width = GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(14.0f), StringUtil.getCjkMain(line.name)) + 8;
                g2d.setColor(new Color(line.color));
                g2d.fillRect(offset - width, 7, width, 21);
                g2d.setColor(GraphicsHelper.foreground(line.color));
                GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(14.0f), StringUtil.getCjkMain(line.name), offset - width / 2, 24, "middle");
                offset -= width + 5;
            }
        } else {
            int offset = 0;
            for (int i = 0; i < interchanges.size(); ++i) {
                ClientCache.ColorNameTuple line = interchanges.get(i);
                if (line == null) continue;
                if (RMGMain.isDigitLine(line.name)) {
                    g2d.setColor(new Color(line.color));
                    g2d.fillRect(offset, 7, 20, 21);
                    g2d.setColor(GraphicsHelper.foreground(line.color));
                    GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(14.0f), RMGMain.getDigitLine(line.name), offset + 10, 24, "middle");
                    offset += 25;
                    continue;
                }
                int width = GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(14.0f), StringUtil.getCjkMain(line.name)) + 8;
                g2d.setColor(new Color(line.color));
                g2d.fillRect(offset, 7, width, 21);
                g2d.setColor(GraphicsHelper.foreground(line.color));
                GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(14.0f), StringUtil.getCjkMain(line.name), offset + width / 2, 24, "middle");
                offset += width + 5;
            }
        }
    }

    private static int calculateInterval(int stnCount, int width) {
        return (width - 370) / (stnCount - 1);
    }
}

