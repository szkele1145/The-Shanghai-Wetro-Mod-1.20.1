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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import mtr.client.ClientData;
import mtr.data.Route;
import com.mojang.blaze3d.platform.NativeImage;

public class ExtensionGraphics {
    private static NativeImage getSHRoutesI(List<Long> routeIds) throws IOException {
        ByteArrayInputStream ist;
        int i;
        List<Route> routes = routeIds.stream().map(ClientData.DATA_CACHE.routeIdMap::get).sorted((route1, route2) -> Collator.getInstance(Locale.CHINA).compare(route1.name, route2.name)).toList();
        int width = 15;
        boolean dlflag = false;
        ArrayList<String> blocks = new ArrayList<String>();
        BufferedImage image = new BufferedImage(1, 1, 2);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (i = 0; i < routes.size(); ++i) {
            if (RMGMain.isDigitLine(routes.get((int)i).name)) {
                width += 90;
                blocks.add(RMGMain.getDigitLine(routes.get((int)i).name));
                continue;
            }
            if (!dlflag) {
                width += 10;
                width += Math.max(GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.font.deriveFont(20.0f), "Line " + String.join((CharSequence)", ", blocks)), GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(36.0f), "\u53f7\u7ebf"));
                dlflag = true;
            }
            width += 20;
            width += Math.max(GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.font.deriveFont(18.0f), StringUtil.getEngMain(routes.get((int)i).name)), GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(28.0f), StringUtil.getCjkMain(routes.get((int)i).name)));
        }
        image = new BufferedImage(width, 100, 2);
        g2d = image.createGraphics();
        dlflag = false;
        width = 0;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (i = 0; i < routes.size(); ++i) {
            if (RMGMain.isDigitLine(routes.get((int)i).name)) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(15 + width, 15, 80, 70);
                g2d.setColor(new Color(routes.get((int)i).color));
                g2d.fillRect(16 + width, 16, 76, 66);
                g2d.setColor(GraphicsHelper.foreground(routes.get((int)i).color));
                GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(72.0f), RMGMain.getDigitLine(routes.get((int)i).name), 15 + width / 2, 85, "middle");
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
            int dw = Math.max(GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.font.deriveFont(18.0f), StringUtil.getEngMain(routes.get((int)i).name)), GraphicsHelper.measureImageText(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(28.0f), StringUtil.getCjkMain(routes.get((int)i).name)));
            g2d.setColor(Color.WHITE);
            g2d.fillRect(15 + (width += 20), 15, dw + 20, 70);
            g2d.setColor(new Color(routes.get((int)i).color));
            g2d.fillRect(16 + width, 16, dw + 20, 66);
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.fontCjk.deriveFont(28.0f), StringUtil.getCjkMain(routes.get((int)i).name), 16 + width + dw / 2 + 10, 70, "middle");
            GraphicsHelper.drawImageTextAlign(g2d, GraphicsCache.CACHE.font.deriveFont(18.0f), StringUtil.getEngMain(routes.get((int)i).name), 16 + width + dw / 2 + 10, 85, "middle");
            width += dw;
        }
        g2d.dispose();
        NativeImage nat = new NativeImage(NativeImage.Format.RGBA, width, 100, false);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write((RenderedImage)image, "png", os);
            ist = new ByteArrayInputStream(os.toByteArray());
        }
        catch (IOException e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
            return nat;
        }
        return NativeImage.read((InputStream) ist);
    }

    public static GraphicsCache.DynamicResource getSHRoutes(List<Long> routeIds) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            return null;
        }
        MessageDigest finalMd = md;
        routeIds.forEach(r -> {
            finalMd.update(r.toString().getBytes());
            finalMd.update("0000".getBytes());
        });
        return GraphicsCache.CACHE.getResource(String.format("sh_route_square_%s", new BigInteger(1, finalMd.digest()).toString(16)), () -> {
            try {
                return ExtensionGraphics.getSHRoutesI(routeIds);
            }
            catch (IOException e) {
                return null;
            }
        });
    }
}

