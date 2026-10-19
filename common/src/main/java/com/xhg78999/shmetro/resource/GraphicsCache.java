package com.xhg78999.shmetro.resource;

import com.xhg78999.shmetro.Main;
import com.xhg78999.shmetro.resource.BlockbenchModel;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
    public final HashMap<String, BlockbenchModel> models = new HashMap();

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
        // 标记重载时刻: 之前启动的异步任务结果作废, 不得覆盖重载后的新条目。
        this.reloadEpoch.incrementAndGet();
    }

    public void reload(ResourceManager resourceManager) {
        com.xhg78999.shmetro.Main.LOGGER.info("Reloading resources");
        this.reloadDynamic();
        this.models.clear();
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
                    this.models.put(name, new BlockbenchModel((String)content));
                });
            }
            catch (Exception e) {
                Main.LOGGER.error(String.format("MTR-SHM: ERROR Happened while loading Blockbench Model %s, skipping", id.toString()));
                com.xhg78999.shmetro.Main.LOGGER.catching(e);
            }
        }
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
            DynamicResource staticTextureProviderOld = this.dynamicResources.get(keyFinal);
            if (staticTextureProviderOld != null && staticTextureProviderOld != DEFAULT_WHITE_DYN_RESOURCE) {
                this.textureRegistryQueue.add(staticTextureProviderOld::remove);
            }
            if (this.reloadEpoch.get() != epoch) {
                // 重载发生: 丢弃本任务结果(基于旧资源), 不注册纹理、不覆盖新条目。
                return;
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

        private void remove() {
            if (!this.resourceLocation.equals(DEFAULT_WHITE_RESOURCE)) {
                TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                textureManager.release(this.resourceLocation);
                AbstractTexture abstractTexture = textureManager.getTexture(this.resourceLocation);
                if (abstractTexture != null) {
                    abstractTexture.releaseId();
                    abstractTexture.close();
                }
            }
        }
    }
}

