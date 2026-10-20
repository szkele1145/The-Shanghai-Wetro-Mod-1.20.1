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
    // 纹理 register/release 必须在主线程执行(对齐 MTR ClientCache.resourceRegistryQueue 架构)。
    // 原实现在 ForkJoinPool 异步线程直接调 TextureManager.register, 与主线程并发读写
    // 普通 HashMap —— 两处线程安全隐患。队列由主线程 getResource/reloadDynamic 消费。
    private final ConcurrentHashMap<String, DynamicResource> dynamicResources = new ConcurrentHashMap();
    private final ConcurrentLinkedQueue<Runnable> textureRegistryQueue = new ConcurrentLinkedQueue();
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
        this.textureRegistryQueue.clear();
        // 释放旧纹理: 直接 clear() 会丢弃引用导致 GL 纹理泄漏,
        // 故逐个排入主线程队列执行 remove()。
        for (DynamicResource dr : this.dynamicResources.values()) {
            if (dr != DEFAULT_WHITE_DYN_RESOURCE) {
                this.textureRegistryQueue.add(dr::remove);
            }
        }
        this.dynamicResources.clear();
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
            return dynamicResource;
        }
        this.dynamicResources.put(key, DEFAULT_WHITE_DYN_RESOURCE);
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
            }
            if (nativeImage == null) {
                dynamicResourceNew = DEFAULT_WHITE_DYN_RESOURCE;
            } else {
                DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
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
                this.textureRegistryQueue.add(() -> minecraftClient.getTextureManager().register(resourceLocation, (AbstractTexture)dynamicTexture));
                dynamicResourceNew = new DynamicResource(resourceLocation, dynamicTexture);
            }
            this.dynamicResources.put(keyFinal, dynamicResourceNew);
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

    public static void read(ResourceManager manager, String path, Consumer<String> callback) {        try {
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

