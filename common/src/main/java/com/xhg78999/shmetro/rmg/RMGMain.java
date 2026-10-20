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

    public static NativeImage getRunin(long platformId, boolean hasLeft, boolean flip) {
        Route route = null;
        try {
            route = RMGMain.getRouteStream(platformId).get(0);
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
            return null;
        }
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
        Route route = null;
        try {
            route = RMGMain.getRouteStream(platformId).get(0);
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
            return null;
        }
        Platform platform = (Platform)ClientData.DATA_CACHE.platformIdMap.get(platformId);
        try {
            return SHDestination.generate(hasLeft, flip, route, width, platform);
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
            return null;
        }
    }

    public static NativeImage getRailMap(long platformId, boolean hasLeft, boolean flip, int width) {
        Route route = null;
        try {
            route = RMGMain.getRouteStream(platformId).get(0);
        }
        catch (Exception e) {
            return null;
        }
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
            if (route.circularState != Route.CircularState.NONE) {
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
            if (route.circularState != Route.CircularState.NONE) {
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
        ((Map<Integer, ClientCache.ColorNameTuple>) ClientData.DATA_CACHE.stationIdToRoutes.get(stationId)).forEach((rid, rc) -> {
            if (!colorCache.contains(rc.color)) {
                colorCache.add(rc.color);
                res.add(rc);
            }
        });
        return res.stream().sorted((route1, route2) -> CHINA_COLLATOR.compare(route1.name, route2.name)).collect(Collectors.toList());
    }

    public static boolean isDigitLine(String name) {
        return StringUtil.getCjkMain(name).contains("\u53f7\u7ebf");
    }

    public static String getDigitLine(String name) {
        return StringUtil.getCjkMain(name).replace("\u53f7\u7ebf", "");
    }
}

