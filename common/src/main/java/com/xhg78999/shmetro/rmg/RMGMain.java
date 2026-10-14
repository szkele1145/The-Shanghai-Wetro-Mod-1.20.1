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
        return GraphicsCache.CACHE.getResource(String.format("rmg_railmap_%s_%s_%s_%s", platformId, hasLeft, flip, width), () -> RMGMain.getRailMap(platformId, hasLeft, flip, width));
    }

    public static GraphicsCache.DynamicResource getRuninCache(long platformId, boolean hasLeft, boolean flip) {
        return GraphicsCache.CACHE.getResource(String.format("rmg_runin_%s_%s_%s", platformId, hasLeft, flip), () -> RMGMain.getRunin(platformId, hasLeft, flip));
    }

    public static GraphicsCache.DynamicResource getDestinationCache(long platformId, boolean hasLeft, boolean flip, int width) {
        return GraphicsCache.CACHE.getResource(String.format("rmg_destination_%s_%s_%s_%s", platformId, hasLeft, flip, width), () -> RMGMain.getDestination(platformId, hasLeft, flip, width));
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
        return res.stream().sorted((route1, route2) -> Collator.getInstance(Locale.CHINA).compare(route1.name, route2.name)).collect(Collectors.toList());
    }

    public static boolean isDigitLine(String name) {
        return StringUtil.getCjkMain(name).contains("\u53f7\u7ebf");
    }

    public static String getDigitLine(String name) {
        return StringUtil.getCjkMain(name).replace("\u53f7\u7ebf", "");
    }
}

