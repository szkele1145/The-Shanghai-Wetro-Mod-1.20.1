package com.xhg78999.shmetro.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mtr.client.CustomResources;
import mtr.render.RenderRailwaySign;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * 指示牌外观的查询缓存。
 *
 * <p>{@link RenderRailwaySign#getSign(String)} 的实现是: 先用
 * {@code BlockRailwaySign.SignType.valueOf(signId)} 走内置类型分支, 失败才
 * 回落到玩家资源包里的 {@code CustomResources.CUSTOM_SIGNS}。</p>
 *
 * <p>问题在于「失败」这条路径: {@code valueOf} 对自定义 sign 会抛
 * {@link IllegalArgumentException}, MTR 在方法内部把它 catch 掉。这意味着
 * 每查询一次自定义 sign, 就会构造一个带完整栈追踪的异常对象 —— 而
 * {@code fillInStackTrace} 的开销随栈深度增长。</p>
 *
 * <p>而本 mod 在渲染热路径上大量使用它:</p>
 * <ul>
 *   <li>{@code RenderShanghaiSign}: 世界里每块指示牌<b>每帧</b>都要查,
 *       查背景色一次、绘制时每格再一次;</li>
 *   <li>{@code GuiShanghaiSign}: 编辑界面每帧遍历全部可选 sign,
 *       且绘制回调里还要再查一次。</li>
 * </ul>
 *
 * <p>视野里有若干块指示牌、玩家又装了自定义 sign 时, 每秒会因此构造
 * 上千个异常对象, 徒增 GC 压力。</p>
 *
 * <p>因此这里缓存 signId -&gt; CustomSign 的映射。缓存的内容是
 * {@code CustomResources.CUSTOM_SIGNS} 的快照, 资源包重载后会被清空
 * (通过 MTR 的 {@link CustomResources#registerReloadListener}), 以便
 * 玩家换资源包后能立刻看到新的外观。</p>
 */
public class SignCache {
    /** signId -&gt; CustomSign。null 表示「查不到」, 也需要缓存, 否则每次都要重查。 */
    private static final Map<String, CustomResources.CustomSign> CACHE = new ConcurrentHashMap<String, CustomResources.CustomSign>();
    /** 已知「查不到」的 signId, 与 CACHE 分开存放 —— ConcurrentHashMap 不允许 null 值。 */
    private static final java.util.Set<String> MISSING = ConcurrentHashMap.newKeySet();
    /** 是否已经向 MTR 注册过资源重载回调, 避免每次开界面都重复注册。 */
    private static volatile boolean reloadListenerRegistered = false;

    /**
     * 查询指示牌外观, 带缓存。
     *
     * <p>语义与 {@link RenderRailwaySign#getSign(String)} 完全一致:
     * 内置类型返回对应的 CustomSign, 自定义 sign 返回资源包里的定义,
     * 两者都找不到时返回 {@code null}。</p>
     *
     * @param signId 指示牌类型标识, 可为 {@code null}
     * @return 对应的外观定义, 不存在时为 {@code null}
     */
    public static CustomResources.CustomSign getSign(String signId) {
        if (signId == null) {
            return null;
        }
        CustomResources.CustomSign cached = CACHE.get(signId);
        if (cached != null) {
            return cached;
        }
        if (MISSING.contains(signId)) {
            return null;
        }
        CustomResources.CustomSign sign = RenderRailwaySign.getSign(signId);
        if (sign == null) {
            MISSING.add(signId);
        }
        else {
            CACHE.put(signId, sign);
        }
        return sign;
    }

    /**
     * 清空缓存。资源包重载后调用 —— 此时玩家可能换掉了自定义指示牌的
     * 定义, 旧的缓存必须作废。
     */
    public static void invalidate() {
        CACHE.clear();
        MISSING.clear();
    }

    /**
     * 首次调用时向 MTR 注册资源重载回调, 之后不再重复注册。
     *
     * <p>MTR 的 {@code RELOAD_LISTENERS} 是静态列表, 重复注册会让每次
     * 资源重载都多跑一遍回调, 所以这里用一个标志位保证只注册一次。</p>
     */
    public static void ensureReloadListenerRegistered() {
        if (reloadListenerRegistered) {
            return;
        }
        synchronized (SignCache.class) {
            if (reloadListenerRegistered) {
                return;
            }
            CustomResources.registerReloadListener((ResourceManager manager) -> SignCache.invalidate());
            reloadListenerRegistered = true;
        }
    }
}
