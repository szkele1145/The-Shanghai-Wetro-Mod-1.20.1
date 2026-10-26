package com.xhg78999.shmetro.resource;

import com.xhg78999.shmetro.resource.GraphicsCache;
import com.xhg78999.shmetro.resource.GraphicsHelper;
import com.xhg78999.shmetro.rmg.RMGMain;
import com.xhg78999.shmetro.utils.StringUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import mtr.client.ClientData;
import mtr.data.Route;
import com.mojang.blaze3d.platform.NativeImage;

public class ExtensionGraphics {
    /**
     * 中文排序用的比较器。
     *
     * <p>{@link Collator#getInstance(Locale)} 每次调用都会新建一个 Collator 对象,
     * 而原实现把它写在 {@code sorted(...)} 的 comparator 里 —— 比较器每比较一次
     * 就调一次, 排序 n 个元素会创建 O(n log n) 个 Collator。这里提到静态常量
     * 共享: {@code Collator} 实例不可变、compare 线程安全, 排序结果完全一致。
     */
    private static final Collator CHINA_COLLATOR = Collator.getInstance(Locale.CHINA);

    private static NativeImage getSHRoutesI(List<Long> routeIds) throws IOException {
        ByteArrayInputStream ist;
        int i;
        List<Route> routes = routeIds.stream().map(ClientData.DATA_CACHE.routeIdMap::get).filter(java.util.Objects::nonNull).sorted((route1, route2) -> {
            String n1 = route1.name == null ? "" : route1.name;
            String n2 = route2.name == null ? "" : route2.name;
            return CHINA_COLLATOR.compare(n1, n2);
        }).toList();
        int width = 15;
        boolean dlflag = false;
        ArrayList<String> blocks = new ArrayList<String>();
        BufferedImage image = new BufferedImage(1, 1, 2);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (i = 0; i < routes.size(); ++i) {
            String rname = routes.get((int)i).name;
            if (RMGMain.isDigitLine(rname)) {
                width += 90;
                blocks.add(RMGMain.getDigitLine(rname));
                continue;
            }
            if (!dlflag) {
                width += 10;
                width += Math.max(GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.font.deriveFont(20.0f), "Line " + String.join((CharSequence)", ", blocks)), GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(36.0f), "\u53f7\u7ebf"));
                dlflag = true;
            }
            width += 20;
            width += Math.max(GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.font.deriveFont(18.0f), StringUtil.getEngMain(rname)), GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(28.0f), StringUtil.getCjkMain(rname)));
        }
        // 【1.1.57 修 AWT 资源泄漏】上面的 1x1 探测图只用来量文字宽度,
        // 真正画图时会被换掉 —— 但它的 Graphics2D 从未 dispose()。
        // Graphics2D 持有 AWT 的渲染状态(字体 hint、颜色栈、native peer),
        // 不释放会一直挂在 AWT 的 Disposable/Disposer 队列上, 直到 GC
        // 也不一定回收 (AWT 的引用是强引用) => 每生成一张线路图泄漏一份。
        // 这里在换图之前显式释放。
        g2d.dispose();
        image = new BufferedImage(width, 100, 2);
        g2d = image.createGraphics();
        dlflag = false;
        width = 0;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (i = 0; i < routes.size(); ++i) {
            String rname2 = routes.get((int)i).name;
            if (RMGMain.isDigitLine(rname2)) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(15 + width, 15, 80, 70);
                g2d.setColor(new Color(routes.get((int)i).color));
                g2d.fillRect(16 + width, 16, 76, 66);
                g2d.setColor(GraphicsHelper.foreground(routes.get((int)i).color));
                GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(72.0f), RMGMain.getDigitLine(rname2), 15 + width / 2, 85, "middle");
                width += 90;
                continue;
            }
            if (!dlflag) {
                g2d.setColor(Color.WHITE);
                GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(36.0f), "\u53f7\u7ebf", 15 + (width += 10), 85, "left");
                GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(20.0f), "Line " + String.join((CharSequence)", ", blocks), 15 + width, 85, "left");
                width += Math.max(GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.font.deriveFont(20.0f), "Line " + String.join((CharSequence)", ", blocks)), GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(36.0f), "\u53f7\u7ebf"));
                dlflag = true;
            }
            int dw = Math.max(GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.font.deriveFont(18.0f), StringUtil.getEngMain(rname2)), GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(28.0f), StringUtil.getCjkMain(rname2)));
            g2d.setColor(Color.WHITE);
            g2d.fillRect(15 + (width += 20), 15, dw + 20, 70);
            g2d.setColor(new Color(routes.get((int)i).color));
            g2d.fillRect(16 + width, 16, dw + 20, 66);
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(28.0f), StringUtil.getCjkMain(rname2), 16 + width + dw / 2 + 10, 70, "middle");
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(18.0f), StringUtil.getEngMain(rname2), 16 + width + dw / 2 + 10, 85, "middle");
            width += dw;
        }
        g2d.dispose();
        // 【1.1.57 修 NativeImage 堆外内存泄漏】
        // 之前这里有一行
        //     NativeImage nat = new NativeImage(NativeImage.Format.RGBA, width, 100, false);
        // 只是为了在 ImageIO 失败时有个返回值兜底, 正常路径上它的堆外内存
        // (NativeImage 底层是 memAlloc) 会被整个丢弃 —— 每生成一张线路图
        // 就白漏一块堆外内存(几百 KB 起)。
        // 改成: 正常路径不提前分配, 直接由 NativeImage.read 返回结果;
        // 异常路径才临时分配一张有效的 1x1 交给调用方。
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write((RenderedImage)image, "png", os);
            ist = new ByteArrayInputStream(os.toByteArray());
        }
        catch (IOException e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
            // 返回一张有效的 1x1 兜底图(所有权交给调用方 —— 调用方会把它
            // 包进 DynamicTexture, 由 DynamicTexture 负责释放)。
            // 注意这里不能 close() 后再返回: NativeImage.close() 释放的是
            // 堆外内存, 交还一个已释放的对象给调用方 = use-after-free。
            return new NativeImage(NativeImage.Format.RGBA, 1, 1, false);
        }
        return NativeImage.read((InputStream) ist);
    }

    public static GraphicsCache.DynamicResource getSHRoutes(List<Long> routeIds) {
        // 热路径: mixin 每帧调用. 拼接 key (routeIds 顺序稳定) 替代 MessageDigest+String.format
        StringBuilder keyBuilder = new StringBuilder("sh_route_square_");
        for (Long r : routeIds) {
            keyBuilder.append(r).append('_');
        }
        String key = keyBuilder.toString();
        return GraphicsCache.CACHE.getResource(key, () -> {
            try {
                return ExtensionGraphics.getSHRoutesI(routeIds);
            }
            catch (IOException e) {
                return null;
            }
        });
    }
}

