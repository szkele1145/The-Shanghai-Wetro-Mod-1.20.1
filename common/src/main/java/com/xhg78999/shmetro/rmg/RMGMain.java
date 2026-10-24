package com.xhg78999.shmetro.rmg;

import com.xhg78999.shmetro.resource.GraphicsCache;
import com.xhg78999.shmetro.rmg.generator.SHDestination;
import com.xhg78999.shmetro.rmg.generator.SHRouteMap;
import com.xhg78999.shmetro.rmg.generator.SHRouteMapLoopline;
import com.xhg78999.shmetro.rmg.generator.SHRunin;
import com.xhg78999.shmetro.utils.StringUtil;
import java.text.Collator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import mtr.client.ClientCache;
import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.data.Station;
import com.mojang.blaze3d.platform.NativeImage;

public class RMGMain {
    /**
     * RMG 贴图宽度的硬上限。
     *
     * <p>贴图宽度由 PSD 顶板的连续排布长度推算而来
     * ({@code RenderShanghaiPSDTop} 里的 {@code width / height * 400}),
     * 正常玩法下十几格就到头了。但推算链条里每一步都依赖世界里的方块排布,
     * 一旦某个环节算出异常值(例如一个巨大的连续同类方块串),
     * {@code new BufferedImage(width, 400)} 就会按那个宽度直接分配内存
     * 并逐像素绘制 —— 32k 宽的图大约 50 MB, 再大就开始拖垮客户端。
     * 这里把宽度钳到 32768: 正常贴图(几万分之一都碰不到)完全不受影响,
     * 异常值也不会把客户端拖死。</p>
     */
    public static final int MAX_IMAGE_WIDTH = 32768;

    /**
     * 中文排序用的比较器。
     *
     * <p>{@link Collator#getInstance(Locale)} 每次调用都会新建一个 Collator 对象
     * (底层规则表是共享的, 但对象本身不是), 而原实现把它写在
     * {@code sorted(...)} 的 comparator 里 —— 比较器每比较一次就调一次,
     * 于是排序 n 个元素会创建 O(n log n) 个 Collator。
     *
     * <p>线路图贴图生成会对每条线路的每个站调用
     * {@link #getInterchangeRoutes}, 一条 20 站的线路就是几百次创建。
     * {@code Collator} 实例是不可变的、且 compare 方法线程安全, 因此可以
     * 提成静态常量共享 —— 排序结果与原来完全一致。
     */
    private static final Collator CHINA_COLLATOR = Collator.getInstance(Locale.CHINA);

    /** 钳制 RMG 贴图宽度到硬上限, 越界值(负数/0/超大)统一按上限处理。 */
    public static int clampWidth(int width) {
        return width <= 0 ? MAX_IMAGE_WIDTH : Math.min(width, MAX_IMAGE_WIDTH);
    }


    public static NativeImage getRunin(long platformId, boolean hasLeft, boolean flip) {
        List<Route> routeList = RMGMain.getRouteStream(platformId);
        if (routeList.isEmpty()) {
            return null;
        }
        Route route = routeList.get(0);
        Station now = RMGMain.getStationFromPlatform(platformId);
        Station past = RMGMain.getStationPast(route, platformId);
        Station next = RMGMain.getStationNext(route, platformId);
        try {
            return SHRunin.generate(hasLeft, next, past, now, route, flip);
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
            return null;
        }
    }

    public static NativeImage getDestination(long platformId, boolean hasLeft, boolean flip, int width) {
        List<Route> routeList = RMGMain.getRouteStream(platformId);
        if (routeList.isEmpty()) {
            return null;
        }
        Route route = routeList.get(0);
        // 【1.1.88 补 null 守卫】平台信息缺失时跳过生成, 避免下游 NPE
        Platform platform = (Platform)ClientData.DATA_CACHE.platformIdMap.get(platformId);
        if (platform == null) {
            return null;
        }
        try {
            return SHDestination.generate(hasLeft, flip, route, width, platform);
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
            return null;
        }
    }

    public static NativeImage getRailMap(long platformId, boolean hasLeft, boolean flip, int width) {
        List<Route> routeList = RMGMain.getRouteStream(platformId);
        if (routeList.isEmpty()) {
            return null;
        }
        Route route = routeList.get(0);
        Station now = RMGMain.getStationFromPlatform(platformId);
        int nowPos = route.getPlatformIdIndex(platformId);
        try {
            if (route.circularState != Route.CircularState.NONE) {
                return SHRouteMapLoopline.generate(hasLeft, flip, now, route, width, nowPos);
            }
            return SHRouteMap.generate(hasLeft, flip, now, route, width, nowPos);
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
            return null;
        }
    }

    public static GraphicsCache.DynamicResource getRailMapCache(long platformId, boolean hasLeft, boolean flip, int width) {
        return GraphicsCache.CACHE.getResource("rmg_railmap_" + platformId + "_" + hasLeft + "_" + flip + "_" + width, () -> RMGMain.getRailMap(platformId, hasLeft, flip, width));
    }

    public static GraphicsCache.DynamicResource getRuninCache(long platformId, boolean hasLeft, boolean flip) {
        return GraphicsCache.CACHE.getResource("rmg_runin_" + platformId + "_" + hasLeft + "_" + flip, () -> RMGMain.getRunin(platformId, hasLeft, flip));
    }

    public static GraphicsCache.DynamicResource getDestinationCache(long platformId, boolean hasLeft, boolean flip, int width) {
        return GraphicsCache.CACHE.getResource("rmg_destination_" + platformId + "_" + hasLeft + "_" + flip + "_" + width, () -> RMGMain.getDestination(platformId, hasLeft, flip, width));
    }

    public static List<Route> getRouteStream(long platformId) {
        return ClientData.ROUTES.stream().map(route -> {
            int index = route.getPlatformIdIndex(platformId);
            if (index < 0 || route.isHidden) {
                return null;
            }
            return route;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static Station getStationFromPlatform(long platformId) {
        return (Station)ClientData.DATA_CACHE.platformIdToStation.get(platformId);
    }

    public static Station getStationPast(Route route, long platformId) {
        try {
            return RMGMain.getStationFromPlatform(((Route.RoutePlatform)route.platformIds.get((int)(route.getPlatformIdIndex((long)platformId) - 1))).platformId);
        }
        catch (Exception e) {
            // 【1.1.58 补 size 防护】环线回退取站台时用 get(size - 2) —— 当环线
            // 只有 1 个站台时 get(-1) 抛 IndexOutOfBoundsException, 而这个异常
            // 发生在 catch 块内部, 会一路逃出 getRunin (第 49 行的调用在 try 之外)
            // => 被 GraphicsCache.getResource 的 catch 兜住 => 整张进站图降级成
            // 白图, 还刷一条错误日志。
            // 站台数不足 2 时没有「上一站」可取, 返回 null 让调用方跳过对应面板
            // (SHRunin.drawPrevStn 已有 null 守卫) —— 与 1.1.46/1.1.57 在
            // SHRunin.generate 内部加的 size>=2 防护是同一件事的两处。
            if (route.circularState != Route.CircularState.NONE && route.platformIds.size() >= 2) {
                return RMGMain.getStationFromPlatform(((Route.RoutePlatform)route.platformIds.get((int)(route.platformIds.size() - 2))).platformId);
            }
            return null;
        }
    }

    public static Station getStationNext(Route route, long platformId) {
        try {
            return RMGMain.getStationFromPlatform(((Route.RoutePlatform)route.platformIds.get((int)(route.getPlatformIdIndex((long)platformId) + 1))).platformId);
        }
        catch (Exception e) {
            // 同 getStationPast: 环线回退到 get(0), 站台数为 0 时会越界。
            if (route.circularState != Route.CircularState.NONE && !route.platformIds.isEmpty()) {
                return RMGMain.getStationFromPlatform(((Route.RoutePlatform)route.platformIds.get((int)0)).platformId);
            }
            return null;
        }
    }

    public static long getStationId(long platformId) {
        Station station = (Station)ClientData.DATA_CACHE.platformIdToStation.get(platformId);
        return station == null ? -1L : station.id;
    }

    public static String getPlatformStationName(long platformId) {
        Station station = (Station)ClientData.DATA_CACHE.platformIdToStation.get(platformId);
        return station == null ? "" : station.name;
    }

    public static String getStationName(Station station) {
        return station == null ? "" : station.name;
    }

    public static List<ClientCache.ColorNameTuple> getInterchangeRoutes(long stationId, Route route) {
        HashSet<ClientCache.ColorNameTuple> res = new HashSet<>();
        HashSet<Integer> colorCache = new HashSet<Integer>();
        colorCache.add(route.color);
        Map<Integer, ClientCache.ColorNameTuple> routeMap = (Map<Integer, ClientCache.ColorNameTuple>) ClientData.DATA_CACHE.stationIdToRoutes.get(stationId);
        if (routeMap != null) {
            routeMap.forEach((rid, rc) -> {
                if (rc != null && !colorCache.contains(rc.color)) {
                    colorCache.add(rc.color);
                    res.add(rc);
                }
            });
        }
        return res.stream().filter(Objects::nonNull).sorted((route1, route2) -> {
            String n1 = route1.name == null ? "" : route1.name;
            String n2 = route2.name == null ? "" : route2.name;
            return CHINA_COLLATOR.compare(n1, n2);
        }).collect(Collectors.toList());
    }

    public static boolean isDigitLine(String name) {
        return StringUtil.getCjkMain(name).contains("\u53f7\u7ebf");
    }

    public static String getDigitLine(String name) {
        return StringUtil.getCjkMain(name).replace("\u53f7\u7ebf", "");
    }
}

