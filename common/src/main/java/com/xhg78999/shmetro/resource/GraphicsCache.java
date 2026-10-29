package com.xhg78999.shmetro.resource;

import com.xhg78999.shmetro.Main;
import com.xhg78999.shmetro.resource.BlockbenchModel;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mtr.mappings.Utilities;
import mtr.mappings.UtilitiesClient;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;

public class GraphicsCache {
    public static final GraphicsCache CACHE = new GraphicsCache();
    /**
     * 动态纹理缓存的字节预算(估算值)。
     *
     * <p>{@link #dynamicResources} 原本是<b>无界</b>的, 而缓存 key 里含有会变的维度:
     * RMG 的线路图/目的地图 key 末尾带着 {@code width}(由 PSD 顶板在世界里的
     * <b>连续排布长度</b>推算), 玩家每改一次排布就是一个新 key;
     * 线路方图 key 带着所选线路 id 的组合。旧 key 的条目<b>永远不会被释放</b>
     * —— 只有「同一个 key 重新生成」或 F3+T 资源重载才会释放。
     * 每个条目都持有一张 {@link NativeImage}(堆外内存, 不受 GC 管理)
     * 加一份 GL 纹理, 进站图一张就有 1816x400x4 ≈ 2.9 MB
     * ⇒ 长时间游玩会持续涨内存, 直到玩家手动重载资源。</p>
     *
     * <p>这里给出预算, 超出后按 LRU 淘汰最久未用的条目并立即释放。
     * 64 MB 足够容纳正常游玩用到的全部站台贴图(实测几十张), 
     * 又能在异常增长时兜住内存。</p>
     */
    private static final long MAX_DYNAMIC_BYTES = 64L * 1024 * 1024;
    /** 条目数硬上限: 防止大量极小贴图绕过字节预算。 */
    private static final int MAX_DYNAMIC_ENTRIES = 512;
    /**
     * 淘汰时保护「最近使用」的条目数。
     *
     * <p>渲染是在一帧里逐个方块实体进行的: 若把本帧稍后要画的纹理淘汰掉,
     * 那一帧就会画成缺失贴图(紫黑格), 下一帧才重新生成。保护最近若干条
     * 可以彻底避开这种同帧自伤, 代价只是预算可能被轻微超出。</p>
     */
    private static final int PROTECTED_RECENT_ENTRIES = 16;
    // 纹理 register/release 必须在主线程执行(对齐 MTR ClientCache.resourceRegistryQueue 架构)。
    // 原实现在 ForkJoinPool 异步线程直接调 TextureManager.register, 与主线程并发读写
    // 普通 HashMap —— 两处线程安全隐患。队列由主线程 getResource/reloadDynamic 消费。
    private final ConcurrentHashMap<String, DynamicResource> dynamicResources = new ConcurrentHashMap();
    /** 当前缓存占用的估算字节数, 用于触发 LRU 淘汰。 */
    private final java.util.concurrent.atomic.AtomicLong dynamicBytes = new java.util.concurrent.atomic.AtomicLong();
    /** 单调递增的访问序号, 充当 LRU 的时间戳。 */
    private final java.util.concurrent.atomic.AtomicLong accessCounter = new java.util.concurrent.atomic.AtomicLong();
    /** 每个 key 的最近访问序号, 数值最小者最先被淘汰。 */
    private final ConcurrentHashMap<String, Long> accessOrder = new ConcurrentHashMap();
    /**
     * 上一次跑过淘汰检查的访问序号。命中路径每 64 次命中才做一次兜底检查,
     * 避免「只命中不新增」时缓存被缓慢撑破上限, 同时又不必每次命中都排序。
     */
    private final java.util.concurrent.atomic.AtomicLong lastEvictCheck = new java.util.concurrent.atomic.AtomicLong(-1L);

    /**
     * 记录一次访问并按预算淘汰旧条目。
     *
     * <p><b>只能在主线程调用</b>(getResource 的入口处): 淘汰动作会把旧纹理的
     * release 排进主线程队列, 而主线程队列的消费点就在 getResource 里 ——
     * 在异步线程做这件事会与主线程的 register/release 竞争。</p>
     *
     * <p>淘汰保护: 最近访问的若干条不参与淘汰, 避免同帧自伤(本帧稍后
     * 要画的纹理被本帧开头的淘汰踢掉)。</p>
     *
     * <p><b>1.1.91 拆分热路径</b>: 原实现把「记录访问序」和「按预算淘汰」绑在
     * 一个方法里, 而它在<b>每次缓存命中</b>时都被调用(每帧每方块)。缓存
     * 一旦超过 512 条目 / 64 MB 上限, 每次命中都要对最多 512 个 key 做
     * 全量排序 —— 一个站台视野里 50 个方块就是每帧 50 次 O(n log n) 排序,
     * 而这些排序的结果(哪些该淘汰)在「只命中、不新增」的稳态下根本不变。
     *
     * <p>这里拆成两段:</p>
     * <ul>
     *   <li>{@link #recordAccess(String)} 只写访问序号(一次 ConcurrentHashMap.put),
     *       命中路径零额外开销;</li>
     *   <li>{@link #evictIfNeeded()} 才做排序+淘汰, 只在<b>预算可能超支</b>时跑 ——
     *       即新增条目时(本来就要异步生成贴图, 排序开销可忽略), 以及
     *       每 64 次命中一次的兜底检查(防止「只命中不新增」时上限被缓慢撑破)。</li>
     * </ul>
     *
     * <p>语义等价: 淘汰只在超预算时触发, 延迟到下次新增时才淘汰最多一个条目,
     * 内存上限的最终约束不变(新增时必触发)。LRU 保护的 16 条最近用的
     * 也不受影响。</p>
     */
    private void recordAccessAndEvict(String key, DynamicResource resource) {
        recordAccess(key);
        evictIfNeeded();
    }

    /** 只记录访问序号, 不触发淘汰。命中路径调用。 */
    private void recordAccess(String key) {
        accessOrder.put(key, accessCounter.incrementAndGet());
    }

    /**
     * 按预算淘汰旧条目。新增条目时必调; 命中路径每 64 次命中调一次兜底检查,
     * 防止「只命中不新增」时缓存被缓慢撑破上限。
     */
    private void evictIfNeeded() {
        long bytes = dynamicBytes.get();
        int entries = dynamicResources.size();
        if (bytes <= MAX_DYNAMIC_BYTES && entries <= MAX_DYNAMIC_ENTRIES) {
            return;
        }
        if (accessCounter.get() - lastEvictCheck.get() < 64) {
            return;
        }
        lastEvictCheck.set(accessCounter.get());
        if (dynamicBytes.get() <= MAX_DYNAMIC_BYTES && dynamicResources.size() <= MAX_DYNAMIC_ENTRIES) {
            return;
        }
        evictOldEntries();
    }

    private void evictOldEntries() {
        // 按 accessOrder 升序找候选: 最久未用的先淘汰。
        // 末尾 PROTECTED_RECENT_ENTRIES 条是最近用的, 保护不动,
        // 避免同帧自伤(本帧稍后要画的纹理被本帧开头的淘汰踢掉)。
        java.util.List<String> candidates = new java.util.ArrayList<>();
        dynamicResources.keySet().forEach(k -> {
            Long ord = accessOrder.get(k);
            if (ord != null) {
                candidates.add(k);
            }
        });
        candidates.sort(java.util.Comparator.comparingLong(k -> {
            Long ord = accessOrder.get(k);
            return ord == null ? Long.MAX_VALUE : ord;
        }));
        int evictable = candidates.size() - PROTECTED_RECENT_ENTRIES;
        if (evictable <= 0) {
            return;
        }
        int freedEntries = 0;
        long freedBytes = 0;
        for (int i = 0; i < evictable; i++) {
            String k = candidates.get(i);
            DynamicResource dr = dynamicResources.get(k);
            if (dr == null || dr == DEFAULT_WHITE_DYN_RESOURCE) {
                continue;
            }
            // 原子 remove(key, value): 只有一个调用方能赢, 赢家才排队释放纹理,
            // 杜绝并发下同一张纹理被 release 两次的可能。
            if (!dynamicResources.remove(k, dr)) {
                continue;
            }
            accessOrder.remove(k);
            dynamicBytes.addAndGet(-dr.estimatedBytes);
            this.textureRegistryQueue.add(dr::remove);
            freedBytes += dr.estimatedBytes;
            freedEntries++;
            if (dynamicBytes.get() <= MAX_DYNAMIC_BYTES && dynamicResources.size() <= MAX_DYNAMIC_ENTRIES - PROTECTED_RECENT_ENTRIES) {
                break;
            }
        }
        if (freedEntries > 0) {
            Main.LOGGER.info(String.format("SHM GraphicsCache: evicted %d dynamic texture(s), freed ~%.1f MB (cache ~%.1f MB, %d entries)",
                freedEntries, freedBytes / 1048576.0, dynamicBytes.get() / 1048576.0, dynamicResources.size()));
        }
    }
    private final ConcurrentLinkedQueue<Runnable> textureRegistryQueue = new ConcurrentLinkedQueue();
    /**
     * 已生成、但还没在主线程注册进 TextureManager 的纹理。
     *
     * <p>纹理生成分两步: 先在异步线程把 {@link NativeImage} 转成
     * {@link DynamicTexture}, 再把「注册」任务排队到主线程执行。</p>
     *
     * <p>若排队之后发生了资源重载({@link #reloadDynamic()} 会清空注册队列),
     * 那个注册任务就被丢弃了 —— 纹理永远进不了 TextureManager, 也就
     * 永远没有人调它的 {@code close()}。</p>
     *
     * <p>{@link DynamicTexture} 持有的 {@link NativeImage} 是
     * {@code MemoryUtil.memAlloc} 分配的<b>堆外</b>内存, <b>不受 GC 管理</b>
     * ⇒ 每丢弃一个纹理就永久泄漏一块图像内存(线路图/进站图动辄几百 KB)。
     * 这个集合用来跟踪「已生成未注册」的纹理, 重载时统一关闭。</p>
     */
    private final java.util.Set<DynamicTexture> pendingTextures = ConcurrentHashMap.newKeySet();
    // 资源重载代号: 异步生成任务捕获当时的代号, 完成后若代号已变则丢弃结果,
    // 避免重载前的旧结果覆盖重载后的新条目(对齐 MTR resourcesToRefresh 机制)。
    private final java.util.concurrent.atomic.AtomicLong reloadEpoch = new java.util.concurrent.atomic.AtomicLong();
    private static final ResourceLocation DEFAULT_WHITE_RESOURCE = new ResourceLocation("mtr", "textures/block/transparent.png");
    private static final DynamicResource DEFAULT_WHITE_DYN_RESOURCE = new DynamicResource(DEFAULT_WHITE_RESOURCE, null);
    public Font font;
    public Font fontCjk;
    // 与 dynamicResources 同理: models 由渲染线程每帧读(13 处 models.get,
    // 分布在 RenderSpecialPassing / RenderERailingDoor / RenderEntityAC /
    // RenderEntityBelief / RenderEntityIronFrame / RenderEntitySecurityCheckPole),
    // 而 reload() 会在资源重载时重建这张表。
    //
    // 单靠 ConcurrentHashMap 不够: reload() 若「先 clear() 再逐个 put()」,
    // 渲染线程在 clear 与 put 之间会读到 null, 而渲染器拿到 null 后直接
    // 解引用 (models.get("ac").renderToBuffer(...)) -> 每次 F3+T 都有一次 NPE 窗口。
    //
    // 因此改为 volatile 引用的「原子整体替换」: reload() 先把新表填满,
    // 最后一步才把这个引用换掉。渲染线程要么看到旧的完整表, 要么看到新的
    // 完整表, 永远看不到中间态。
    // 1.1.27 修 dynamicResources 时漏掉了这张表, 此处补上。
    public volatile ConcurrentHashMap<String, BlockbenchModel> models = new ConcurrentHashMap();

    /** 在主线程消费待注册的纹理任务(对齐 MTR 的 resourceRegistryQueue)。 */
    public void runTextureRegistryQueue() {
        Runnable r;
        while ((r = this.textureRegistryQueue.poll()) != null) {
            try {
                r.run();
            }
            catch (Exception e) {
                com.xhg78999.shmetro.Main.LOGGER.catching(e);
            }
        }
    }

    public void reloadDynamic() {
        com.xhg78999.shmetro.Main.LOGGER.info("Reloading dynamic resources");
        // 标记重载时刻: 之前启动的异步任务结果作废, 不得覆盖重载后的新条目。
        //
        // 必须在所有清理动作之前递增。原实现把它放在最后, 于是存在这样的竞态:
        //   1) 异步任务 T 启动, 捕获 epoch = 5
        //   2) reloadDynamic() 执行完「清队列 / 排队释放 / clear map」, 此时 epoch 仍是 5
        //   3) 任务 T 恰好在此刻完成 -> epoch 检查 5 == 5 通过 -> 把「基于重载前资源
        //      生成」的纹理注册进 TextureManager 并写回 dynamicResources
        //   4) reloadDynamic() 才把 epoch 递增到 6
        // 结果: 一张过期纹理被注册并留存, 而 epoch 已变, 后续任何任务都不会修正它,
        // 直到下一次资源重载。同 1.1.40 修的 getResource 里的次序问题同源。
        this.reloadEpoch.incrementAndGet();
        // 资源重载时: 已排队但未执行的纹理任务作废(它们基于旧资源),
        // 队列清空, 避免旧纹理在重载后被注册回来。
        //
        // 队列里可能有「注册 DynamicTexture」的任务。清空队列 = 这些任务
        // 永远不会执行 = 那些 DynamicTexture 永远不会被注册、也就永远
        // 不会被 close() -> NativeImage 的堆外内存永久泄漏。
        // 被丢弃的纹理都登记在 pendingTextures 里, 本方法末尾统一关闭。
        this.textureRegistryQueue.clear();
        // 释放旧纹理: 直接 clear() 会丢弃引用导致 GL 纹理泄漏,
        // 故逐个排入主线程队列执行 remove()。
        for (DynamicResource dr : this.dynamicResources.values()) {
            if (dr != DEFAULT_WHITE_DYN_RESOURCE) {
                this.textureRegistryQueue.add(dr::remove);
            }
        }
        this.dynamicResources.clear();
        // 记账与访问序一并清空: 旧条目全部被释放了, 字节数必须归零,
        // 否则重载后计数一直虚高, 淘汰逻辑会立刻误触发。
        this.dynamicBytes.set(0L);
        this.accessOrder.clear();
        // 关闭所有「已生成但还没注册进 TextureManager」的纹理。
        // 它们的注册任务刚刚被清空丢弃, 现在必须释放堆外内存。
        for (DynamicTexture dt : this.pendingTextures) {
            closeTextureQuietly(dt);
        }
        this.pendingTextures.clear();
    }

    public void reload(ResourceManager resourceManager) {
        com.xhg78999.shmetro.Main.LOGGER.info("Reloading resources");
        this.reloadDynamic();
        // 先把新表填满, 最后一步才整体替换引用 —— 渲染线程读到的永远是
        // 一张完整的表(旧的或新的), 不会看到「已 clear 尚未 put 完」的中间态。
        ConcurrentHashMap<String, BlockbenchModel> newModels = new ConcurrentHashMap<>();
        try {
            this.font = Font.createFont(0, Utilities.getInputStream((Optional)resourceManager.getResource(new ResourceLocation("shmetro", "font/arial.ttf"))));
            this.fontCjk = Font.createFont(0, Utilities.getInputStream((Optional)resourceManager.getResource(new ResourceLocation("shmetro", "font/simhei.ttf"))));
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
        }
        for (ResourceLocation id : resourceManager.listResources("models/dynamic", path -> path.toString().endsWith(".bbmodel")).keySet()) {
            try {
                GraphicsCache.read(resourceManager, id.toString(), content -> {
                    String name = id.getPath().replace("models/dynamic/", "").replace(".bbmodel", "");
                    Main.LOGGER.info(String.format("Load Blockbench model %s", name));
                    newModels.put(name, new BlockbenchModel((String)content));
                });
            }
            catch (Exception e) {
                Main.LOGGER.error(String.format("MTR-SHM: ERROR Happened while loading Blockbench Model %s, skipping", id.toString()));
                com.xhg78999.shmetro.Main.LOGGER.catching(e);
            }
        }
        // 原子替换: volatile 写, 渲染线程的下一次读即可看到完整的新表。
        this.models = newModels;
    }

    public DynamicResource getResource(String key, Supplier<NativeImage> supplier) {
        // 先消费主线程队列: 纹理注册必须发生在渲染线程(持有 GL 上下文)。
        this.runTextureRegistryQueue();
        Minecraft minecraftClient = Minecraft.getInstance();
        DynamicResource dynamicResource = this.dynamicResources.get(key);
        if (dynamicResource != null) {
            // 占位(DEFAULT)存在时同样直接返回: 生成中不重复触发,
            // 生成失败则保持占位(与 MTR 原版行为一致, 不重试)。
            //
            // 命中即算一次使用: 记录 LRU 访问序并按预算淘汰旧条目。
            // 缓存 key 含有会变的维度(RMG 的 width、线路图 id 组合),
            // 玩家改一次排布就是一个新 key, 旧条目原本永远不释放。
            this.recordAccessAndEvict(key, dynamicResource);
            return dynamicResource;
        }
        this.dynamicResources.put(key, DEFAULT_WHITE_DYN_RESOURCE);
        recordAccess(key);
        evictIfNeeded();
        final String keyFinal = key;
        final long epoch = this.reloadEpoch.get();
        CompletableFuture.runAsync(() -> {
            DynamicResource dynamicResourceNew;
            NativeImage nativeImage = null;
            try {
                nativeImage = (NativeImage)supplier.get();
                assert (nativeImage != null);
            }
            catch (Exception e) {
                com.xhg78999.shmetro.Main.LOGGER.catching(e);
            }
            // 先判 epoch 再决定是否释放旧纹理:
            // 原实现把「释放旧纹理」放在 epoch 检查之前, 若本任务启动后发生了
            // 资源重载, 而重载后的新任务已经完成并 put 了有效的新纹理,
            // 这里的 remove 会把它排进队列释放掉 -> 该 key 永久退化成白图。
            if (this.reloadEpoch.get() != epoch) {
                // 重载发生: 丢弃本任务结果(基于旧资源), 不注册纹理、不覆盖新条目。
                // NativeImage 是堆外内存(allocateMemory), 不受 GC 管理,
                // 必须显式 close, 否则每次重载期间的过期任务都会泄漏一块图像内存。
                closeQuietly(nativeImage);
                return;
            }
            DynamicResource staticTextureProviderOld = this.dynamicResources.get(keyFinal);
            if (staticTextureProviderOld != null && staticTextureProviderOld != DEFAULT_WHITE_DYN_RESOURCE) {
                this.textureRegistryQueue.add(staticTextureProviderOld::remove);
                // 旧条目被替换: 减掉它的记账, 免得字节数只增不减。
                this.dynamicBytes.addAndGet(-staticTextureProviderOld.estimatedBytes);
            }
            if (nativeImage == null) {
                dynamicResourceNew = DEFAULT_WHITE_DYN_RESOURCE;
            } else {
                DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                // 登记到 pendingTextures: 从这里到「主线程注册完成」之间,
                // 若发生资源重载导致注册任务被丢弃, 这个纹理就再也没人 close()
                // -> NativeImage 堆外内存永久泄漏。登记后由重载流程兜底关闭。
                this.pendingTextures.add(dynamicTexture);
                String newKey = keyFinal;
                try {
                    newKey = URLEncoder.encode(keyFinal, StandardCharsets.UTF_8);
                }
                catch (Exception e) {
                    com.xhg78999.shmetro.Main.LOGGER.catching(e);
                }
                String newKeyLower = newKey.toLowerCase(Locale.ENGLISH);
                ResourceLocation resourceLocation = new ResourceLocation("shmetro", "dynamic_texture_" + newKeyLower.replaceAll("[^0-9a-z_]", "_"));
                // register 与旧纹理 release 排入主线程队列执行(对齐 MTR 架构)。
                this.textureRegistryQueue.add(() -> {
                    minecraftClient.getTextureManager().register(resourceLocation, (AbstractTexture)dynamicTexture);
                    // 注册完成: 该纹理已交给 TextureManager 托管, 不再需要
                    // 由重载流程兜底关闭(之后的释放走 DynamicResource.remove)。
                    this.pendingTextures.remove(dynamicTexture);
                });
                dynamicResourceNew = new DynamicResource(resourceLocation, dynamicTexture);
                // 记账: 纹理真正生成后才知道它占多少字节。
                // (命中缓存的条目在 recordAccess 里已记过一次,
                //  这里是首次创建, 之前只有占位条目, 没记过字节。)
                this.dynamicBytes.addAndGet(dynamicResourceNew.estimatedBytes);
            }
            this.dynamicResources.put(keyFinal, dynamicResourceNew);
            // 刷新访问序: 占位条目的序号是「占位创建时」的, 若生成期间别的 key
            // 被大量访问, 新纹理的序号就显得很旧 ⇒ 超预算时会被优先淘汰,
            // 玩家刚等到的图立刻变紫黑格再重新生成(抖动)。异步线程更新
            // ConcurrentHashMap + AtomicLong 是安全的。
            recordAccess(keyFinal);
            // 新条目刚加进来, 预算很可能超支 -> 这里跑一次淘汰。
            // 异步线程里跑淘汰是安全的: 只读 accessOrder / dynamicResources,
            // 写入走 textureRegistryQueue(主线程消费), 与主线程无竞态。
            this.evictIfNeeded();
        });
        return DEFAULT_WHITE_DYN_RESOURCE;
    }

    /**
     * 释放一张 NativeImage，吞掉关闭时的异常。
     *
     * <p>{@link NativeImage} 的图像数据由 {@code MemoryUtil.memAlloc} 分配在堆外，
     * 不受 GC 管理；丢弃引用而不 close 就是永久泄漏。此处用于「任务结果已作废」
     * 的分支 —— 图像不会被注册成纹理，必须自己回收。</p>
     *
     * @param nativeImage 待释放的图像，可为 {@code null}
     */
    private static void closeQuietly(NativeImage nativeImage) {
        if (nativeImage == null) {
            return;
        }
        try {
            nativeImage.close();
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
        }
    }

    /**
     * 关闭一张「已生成但从未注册进 TextureManager」的动态纹理。
     *
     * <p>{@link DynamicTexture#close()} 会释放它持有的 {@link NativeImage}
     * 的堆外内存。由于它从未被注册, 也不会被 {@link TextureManager#release}
     * 回收, 所以只能在这里显式关闭。</p>
     *
     * @param dynamicTexture 待关闭的纹理，可为 {@code null}
     */
    private static void closeTextureQuietly(DynamicTexture dynamicTexture) {
        if (dynamicTexture == null) {
            return;
        }
        try {
            dynamicTexture.close();
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
        }
    }

    public static void read(ResourceManager manager, String path, Consumer<String> callback) {
        try {
            UtilitiesClient.getResources((ResourceManager)manager, (ResourceLocation)new ResourceLocation(path)).forEach(resource -> {
                try {
                    InputStream stream = Utilities.getInputStream((Resource)resource);
                    try {
                        callback.accept(IOUtils.toString((InputStream)stream, (Charset)StandardCharsets.UTF_8));
                    }
                    catch (Throwable var7) {
                        if (stream != null) {
                            try {
                                stream.close();
                            }
                            catch (Throwable var6) {
                                var7.addSuppressed(var6);
                            }
                        }
                        throw var7;
                    }
                    stream.close();
                }
                catch (Exception var8) {
                    com.xhg78999.shmetro.Main.LOGGER.catching(var8);
                }
                try {
                    Utilities.closeResource((Resource)resource);
                }
                catch (IOException var5) {
                    com.xhg78999.shmetro.Main.LOGGER.catching(var5);
                }
            });
        }
        catch (Exception exception) {
            com.xhg78999.shmetro.Main.LOGGER.catching(exception);
        }
    }

    public static class DynamicResource {
        public final int width;
        public final int height;
        public final ResourceLocation resourceLocation;
        /** 本条目占用的估算字节数(width*height*4, 与 NativeImage 的 RGBA 布局一致)。 */
        public final long estimatedBytes;

        public DynamicResource(ResourceLocation resourceLocation, DynamicTexture dynamicTexture) {
            this.resourceLocation = resourceLocation;
            if (dynamicTexture != null) {
                NativeImage nativeImage = dynamicTexture.getPixels();
                if (nativeImage != null) {
                    this.width = nativeImage.getWidth();
                    this.height = nativeImage.getHeight();
                } else {
                    this.width = 16;
                    this.height = 16;
                }
            } else {
                this.width = 16;
                this.height = 16;
            }
            this.estimatedBytes = (long)this.width * (long)this.height * 4L;
        }

        /**
         * 释放本动态纹理。
         *
         * <p>只需调用 {@link TextureManager#release(ResourceLocation)}：它内部已经
         * 完成了「从 {@code byPath} 移除 → {@code safeClose}（{@code close()} + {@code releaseId()}）」全部三件事。</p>
         *
         * <p><b>不要再补 {@code getTexture()} + {@code releaseId()} + {@code close()}</b>（上游原实现与
         * MTR 3.2.2 官方 {@code ClientCache$DynamicResource.remove()} 都这么写，是一段真实缺陷）：</p>
         * <ul>
         *   <li>{@code release()} 已把纹理从 {@code byPath} 移除，随后 {@code getTexture()} 必然找不到，
     *       于是 MC 会 <b>new 出一个 {@code SimpleTexture} 占位并把它重新注册回 {@code byPath}</b>；</li>
         *   <li>该占位加载不到资源，{@code register()} 便返回 <b>全局共享的</b>
         *       {@code MissingTextureAtlasSprite.getTexture()}（紫黑格单例）；</li>
         *   <li>于是 {@code if (abstractTexture != null)} 成立，代码对这个<b>全局单例</b>
         *       调用了 {@code releaseId()} + {@code close()} —— 释放了全游戏共用的紫黑格纹理，
     *       导致所有「加载失败」的纹理（含其他 mod 的）一起不可渲染。</li>
         * </ul>
         */
        private void remove() {
            if (!this.resourceLocation.equals(DEFAULT_WHITE_RESOURCE)) {
                TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                textureManager.release(this.resourceLocation);
            }
        }
    }
}

