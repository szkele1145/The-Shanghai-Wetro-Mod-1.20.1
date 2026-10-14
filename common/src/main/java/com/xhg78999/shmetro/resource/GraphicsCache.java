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
    private final HashMap<String, DynamicResource> dynamicResources = new HashMap();
    private static final ResourceLocation DEFAULT_WHITE_RESOURCE = new ResourceLocation("mtr", "textures/block/transparent.png");
    private static final DynamicResource DEFAULT_WHITE_DYN_RESOURCE = new DynamicResource(DEFAULT_WHITE_RESOURCE, null);
    public Font font;
    public Font fontCjk;
    public final HashMap<String, BlockbenchModel> models = new HashMap();

    public void reloadDynamic() {
        com.xhg78999.shmetro.Main.LOGGER.info("Reloading dynamic resources");
        this.dynamicResources.clear();
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
        Minecraft minecraftClient = Minecraft.getInstance();
        DynamicResource dynamicResource = this.dynamicResources.get(key);
        if (dynamicResource != null) {
            return dynamicResource;
        }
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
            DynamicResource staticTextureProviderOld = this.dynamicResources.get(key);
            if (staticTextureProviderOld != null) {
                staticTextureProviderOld.remove();
            }
            if (nativeImage == null) {
                dynamicResourceNew = DEFAULT_WHITE_DYN_RESOURCE;
            } else {
                DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                String newKey = key;
                try {
                    newKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
                }
                catch (Exception e) {
                    com.xhg78999.shmetro.Main.LOGGER.catching(e);
                }
                String newKeyLower = newKey.toLowerCase(Locale.ENGLISH);
                ResourceLocation resourceLocation = new ResourceLocation("shmetro", "dynamic_texture_" + newKeyLower.replaceAll("[^0-9a-z_]", "_"));
                minecraftClient.getTextureManager().register(resourceLocation, (AbstractTexture)dynamicTexture);
                dynamicResourceNew = new DynamicResource(resourceLocation, dynamicTexture);
            }
            this.dynamicResources.put(key, dynamicResourceNew);
        });
        if (dynamicResource == null) {
            this.dynamicResources.put(key, DEFAULT_WHITE_DYN_RESOURCE);
            return DEFAULT_WHITE_DYN_RESOURCE;
        }
        return dynamicResource;
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
            if (!this.resourceLocation.equals((Object)DEFAULT_WHITE_RESOURCE)) {
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

