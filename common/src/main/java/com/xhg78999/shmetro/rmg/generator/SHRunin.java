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
            // 【1.1.46】环线回退取站台时的两道防护:
            //   1) platformIds.size() < 2 时 line.platformIds.get(size-2) / get(1) 会越界;
            //      站台数为 1 的环线 (现实中不存在, 但 MTR 允许加载) 就会抛
            //      IndexOutOfBoundsException。
            //   2) platformIdToStation.get() 对不存在的 key 返回 null, 再取
            //      .platformId 会 NPE。
            // 两种情况都让「上一站/下一站」取不到, 此时保持 null 即可 ——
            // drawPrevStn / drawNextStn 已有 null 守卫, 会跳过对应面板,
            // 而不是整张图生成失败。
            if (prev == null && line.platformIds.size() >= 2) {
                prev = RMGMain.getStationFromPlatform(((Route.RoutePlatform)line.platformIds.get((int)(line.platformIds.size() - 2))).platformId);
            }
            if (next == null && line.platformIds.size() >= 2) {
                next = RMGMain.getStationFromPlatform(((Route.RoutePlatform)line.platformIds.get((int)1)).platformId);
            }
        }
        SHRunin.drawGeneralStation(g2d, direction, next, prev, now, line, flip);
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
        // 【1.1.83 补 null 站守卫】now 可能为 null（平台站点映射缺失时 getStationFromPlatform 返回 null）。
        // 原实现 RMGMain.getStationName(null) 返回 "" 已有保护, 但为保险起见直接跳过绘制也行。
        // 此处依赖 getStationName(null)="", 保持最小改动。
        Font fontCjk = GraphicsCache.CACHE.fontCjk.deriveFont(112.0f);
        Font fontEng = GraphicsCache.CACHE.font.deriveFont(36.0f);
        g2d.setColor(Color.BLACK);
        GraphicsHelper.drawImageTextAlign(g2d, fontCjk, StringUtil.getCjkMain(RMGMain.getStationName(now)), 908, 250, "middle");
        GraphicsHelper.drawImageTextAlign(g2d, fontEng, StringUtil.getEngMain(RMGMain.getStationName(now)), 908, 300, "middle");
    }

    private static void drawPrevStn(Graphics2D g2d, boolean direction, Station next, Station prev, Station now, Route line, boolean flip) {
        // 守卫必须与各分支实际绘制的站一致: 两个 direction 分支取值互补, 需分别检查。
        // 原实现的守卫 (flip ? prev : next) 只匹配 direction=false 分支;
        // direction=true(箭头朝左)时查的是不画的那个站, 路线端点站会
        // 画空白站名(画 null)或让该显示的面板整块缺失。上游反编译原件同错。
        if (direction) {
            if ((flip ? next : prev) == null) {
                return;
            }
        } else if ((flip ? prev : next) == null) {
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
        // 同 drawPrevStn: 守卫需按 direction 分支各自检查实际绘制的站。
        // 原实现的守卫 (flip ? next : prev) 只匹配 direction=false 分支。
        if (direction) {
            if ((flip ? prev : next) == null) {
                return;
            }
        } else if ((flip ? next : prev) == null) {
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

