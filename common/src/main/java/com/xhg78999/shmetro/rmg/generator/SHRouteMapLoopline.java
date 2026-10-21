package com.xhg78999.shmetro.rmg.generator;

import com.xhg78999.shmetro.resource.GraphicsCache;
import com.xhg78999.shmetro.resource.GraphicsHelper;
import com.xhg78999.shmetro.rmg.RMGMain;
import com.xhg78999.shmetro.utils.StringUtil;
import java.awt.BasicStroke;
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
import net.minecraft.resources.ResourceLocation;

public class SHRouteMapLoopline {
    public static NativeImage generate(boolean direction, boolean flip, Station now, Route line, int width, int nowpos) throws IOException {
        ByteArrayInputStream ist;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, 2);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        SHRouteMapLoopline.drawMap(g2d, direction, !flip, now, line, width, nowpos);
        g2d.dispose();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write((RenderedImage)image, "png", os);
            ist = new ByteArrayInputStream(os.toByteArray());
        }
        catch (IOException e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
            // 【1.1.57 修 NativeImage 堆外内存泄漏】原来这里提前分配了一张
            // 整幅大小的 NativeImage 只为在 ImageIO 失败时兜底, 正常路径上它的
            // 堆外内存(memAlloc, 不受 GC 管)被整个丢弃 => 每生成一张图白漏一块
            // (进站图 1816x400x4 ≈ 2.9 MB)。现在正常路径不提前分配,
            // 异常路径只返回一张有效的 1x1(所有权交给调用方 -> DynamicTexture)。
            return new NativeImage(NativeImage.Format.RGBA, 1, 1, false);
        }
        return NativeImage.read((InputStream) ist);
    }

    private static void drawMap(Graphics2D g2d, boolean direction, boolean flip, Station now, Route line, int width, int nowpos) {
        BasicStroke lineStroke = new BasicStroke(12.0f, 2, 1);
        g2d.setColor(new Color(line.color));
        g2d.setStroke(lineStroke);
        if (line.platformIds.size() <= 3) {
            return;
        }
        if (direction) {
            g2d.drawPolygon(new int[]{262, 412, width - 262, width - 412, 262}, new int[]{353, 272, 272, 353, 353}, 5);
            GraphicsHelper.pasteImage(g2d, new ResourceLocation("shmetro", "textures/rendering/loopline_left.png"), 330, 347);
            GraphicsHelper.pasteImage(g2d, new ResourceLocation("shmetro", "textures/rendering/loopline_right.png"), width - 450, 266);
        } else {
            g2d.drawPolygon(new int[]{412, 262, width - 412, width - 262, 412}, new int[]{353, 272, 272, 353, 353}, 5);
            GraphicsHelper.pasteImage(g2d, new ResourceLocation("shmetro", "textures/rendering/loopline_right.png"), 500, 347);
            GraphicsHelper.pasteImage(g2d, new ResourceLocation("shmetro", "textures/rendering/loopline_left.png"), width - 500, 266);
        }
        SHRouteMapLoopline.drawStationBricks(g2d, direction, flip, now, line, width, nowpos);
    }

    private static void drawStationBricks(Graphics2D g2d, boolean direction, boolean flip, Station now, Route line, int width, int nowpos) {
        List pids = line.platformIds.stream().collect(Collectors.toList());
        if (flip) {
            Collections.reverse(pids);
            if (nowpos != 0) {
                nowpos = pids.size() - nowpos - 2;
            }
        }
        pids.remove(pids.size() - 1);
        if (direction) {
            int lpad = SHRouteMapLoopline.getLeftAndRight(pids.size());
            for (int i = 0; i < pids.size(); ++i) {
                int ym;
                if (i < lpad) {
                    ym = Math.round(150.0f / (float)(lpad + 1));
                    if (i == nowpos) {
                        g2d.setColor(Color.RED);
                        GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(17.0f), "\u672c\u7ad9", 292 + (i + 1) * ym, (int)(359.0 - (double)((i + 1) * ym) * 0.54), "left");
                    }
                    g2d.setColor(new Color(line.color));
                    g2d.fillRect(228 + (i + 1) * ym, (int)(347.0 - (double)((i + 1) * ym) * 0.54), 34, 12);
                    SHRouteMapLoopline.drawStationVLeft(g2d, direction, line, RMGMain.getStationFromPlatform(((Route.RoutePlatform)pids.get((int)i)).platformId), ym * (i + 1), i == nowpos);
                    continue;
                }
                if (i >= lpad && i < pids.size() - lpad) {
                    int base = 412 + i * SHRouteMapLoopline.calculateIntervalUp(pids.size() - 2 * lpad, width);
                    if (i == nowpos) {
                        g2d.setColor(Color.RED);
                        GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(17.0f), "\u672c\u7ad9", base, 297, "middle");
                    }
                    g2d.setColor(new Color(line.color));
                    g2d.fillRect(base - 6, 244, 12, 24);
                    SHRouteMapLoopline.drawStation(g2d, direction, RMGMain.getStationFromPlatform(((Route.RoutePlatform)pids.get((int)i)).platformId), line, base, i == nowpos);
                    continue;
                }
                ym = Math.round(150.0f / (float)(lpad + 1));
                if (i == nowpos) {
                    g2d.setColor(Color.RED);
                    GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(17.0f), "\u672c\u7ad9", width - 442 + (pids.size() - i) * ym, (int)(359.0 - (double)((pids.size() - i) * ym) * 0.54), "right");
                }
                g2d.setColor(new Color(line.color));
                g2d.fillRect(width - 412 + (pids.size() - i) * ym, (int)(347.0 - (double)((pids.size() - i) * ym) * 0.54), 34, 12);
                SHRouteMapLoopline.drawStationVRight(g2d, direction, line, RMGMain.getStationFromPlatform(((Route.RoutePlatform)pids.get((int)i)).platformId), ym * (pids.size() - i), width, i == nowpos);
            }
        } else {
            int lpad = SHRouteMapLoopline.getLeftAndRight(pids.size());
            for (int i = 0; i < pids.size(); ++i) {
                int ym;
                if (i < lpad) {
                    ym = Math.round(150.0f / (float)(lpad + 1));
                    g2d.setColor(new Color(line.color));
                    g2d.fillRect(378 - (i + 1) * ym, (int)(347.0 - (double)((i + 1) * ym) * 0.54), 34, 12);
                    SHRouteMapLoopline.drawStationVLeft(g2d, direction, line, RMGMain.getStationFromPlatform(((Route.RoutePlatform)pids.get((int)i)).platformId), ym * (i + 1), i == nowpos);
                    continue;
                }
                if (i >= lpad && i < pids.size() - lpad) {
                    int base = 412 + i * SHRouteMapLoopline.calculateIntervalUp(pids.size() - 2 * lpad, width);
                    if (i == nowpos) {
                        g2d.setColor(Color.RED);
                        GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(17.0f), "\u672c\u7ad9", base, 297, "middle");
                    }
                    g2d.setColor(new Color(line.color));
                    g2d.fillRect(base - 6, 244, 12, 24);
                    SHRouteMapLoopline.drawStation(g2d, direction, RMGMain.getStationFromPlatform(((Route.RoutePlatform)pids.get((int)i)).platformId), line, base, i == nowpos);
                    continue;
                }
                ym = Math.round(150.0f / (float)(lpad + 1));
                g2d.setColor(new Color(line.color));
                g2d.fillRect(width - 262 - (pids.size() - i) * ym, (int)(347.0 - (double)((pids.size() - i) * ym) * 0.54), 34, 12);
                SHRouteMapLoopline.drawStationVRight(g2d, direction, line, RMGMain.getStationFromPlatform(((Route.RoutePlatform)pids.get((int)i)).platformId), ym * (pids.size() - i), width, i == nowpos);
            }
        }
    }

    private static BufferedImage drawSubGraphics(boolean pivot, Station now, Route line, boolean isNow) {
        BufferedImage image = new BufferedImage(200, 28, 2);
        Graphics2D g2da = image.createGraphics();
        g2da.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2da.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        List<ClientCache.ColorNameTuple> interchanges = RMGMain.getInterchangeRoutes(now.id, line);
        if (isNow) {
            g2da.setColor(Color.RED);
        } else {
            g2da.setColor(Color.BLACK);
        }
        if (pivot) {
            GraphicsHelper.drawImageTextAlign(g2da, GraphicsCache.CACHE.fontCjk.deriveFont(16.0f), StringUtil.getCjkMain(now.name), 6, 16, "left");
            GraphicsHelper.drawImageTextAlign(g2da, GraphicsCache.CACHE.font.deriveFont(8.0f), StringUtil.getEngMain(now.name), 6, 26, "left");
        } else {
            GraphicsHelper.drawImageTextAlign(g2da, GraphicsCache.CACHE.fontCjk.deriveFont(16.0f), StringUtil.getCjkMain(now.name), 200, 16, "right");
            GraphicsHelper.drawImageTextAlign(g2da, GraphicsCache.CACHE.font.deriveFont(8.0f), StringUtil.getEngMain(now.name), 200, 26, "right");
        }
        if (!interchanges.isEmpty()) {
            g2da.setColor(Color.BLACK);
            g2da.drawLine(0, 27, 200, 28);
            SHRouteMapLoopline.drawTransferBricks(g2da, pivot, interchanges);
        }
        g2da.dispose();
        return image;
    }

    private static void drawStationVLeft(Graphics2D g2d, boolean direction, Route line, Station now, int ym, boolean isNow) {
        BufferedImage image = SHRouteMapLoopline.drawSubGraphics(false, now, line, isNow);
        if (direction) {
            g2d.drawImage((Image)image, 20 + ym, (int)(332.0 - (double)ym * 0.54), null);
        } else {
            g2d.drawImage((Image)image, 162 - ym, (int)(332.0 - (double)ym * 0.54), null);
        }
    }

    private static void drawStationVRight(Graphics2D g2d, boolean direction, Route line, Station now, int ym, int width, boolean isNow) {
        BufferedImage image = SHRouteMapLoopline.drawSubGraphics(true, now, line, isNow);
        if (direction) {
            g2d.drawImage((Image)image, width - 377 + ym, (int)(332.0 - (double)ym * 0.54), null);
        } else {
            g2d.drawImage((Image)image, width - 222 - ym, (int)(332.0 - (double)ym * 0.54), null);
        }
    }

    private static void drawStation(Graphics2D g2d, boolean direction, Station now, Route line, int baselineStn, boolean isNow) {
        BufferedImage image = SHRouteMapLoopline.drawSubGraphics(direction, now, line, isNow);
        if (direction) {
            g2d.translate(baselineStn - 16, 230);
            g2d.rotate(-45.0);
            g2d.drawImage((Image)image, 0, 0, null);
            g2d.rotate(45.0);
            g2d.translate(-baselineStn + 16, -230);
        } else {
            g2d.translate(baselineStn - 92, 60);
            g2d.rotate(45.0);
            g2d.drawImage((Image)image, 0, 0, null);
            g2d.rotate(-45.0);
            g2d.translate(-baselineStn + 92, -60);
        }
    }

    private static void drawTransferBricks(Graphics2D g2d, boolean direction, List<ClientCache.ColorNameTuple> interchanges) {
        if (interchanges.isEmpty()) {
            return;
        }
        if (direction) {
            int offset = 200;
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

    private static int getLeftAndRight(int stnCount) {
        return (int)Math.max((float)stnCount * 0.15f, 1.0f);
    }

    private static int calculateIntervalUp(int stnCountRV, int width) {
        return (width - 674) / (stnCountRV + 1);
    }
}

