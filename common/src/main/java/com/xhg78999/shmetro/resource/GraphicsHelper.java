package com.xhg78999.shmetro.resource;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.imageio.ImageIO;
import mtr.data.IGui;
import mtr.mappings.Text;
import mtr.mappings.Utilities;
import mtr.mappings.UtilitiesClient;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import org.joml.Matrix4f;

public class GraphicsHelper {
    public static void drawText(Style style, PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, float sx, float sy, float sz, int maxwidth) {
        // 【1.1.61 加空值防护】text 为 null 时按空串处理。
        // Text.literal(null) 会在 getVisualOrderText() 阶段抛 NPE, 而本方法
        // 的调用方(如 RenderGlobalPosition)处在渲染热路径上, 没有 try/catch ——
        // 一次 NPE 就会打断整个渲染批次, 让该帧的方块实体全部渲染不出来。
        // 空串是「没有文字要画」的自然语义, 不会产生任何可见效果。
        if (text == null) {
            text = "";
        }
        FormattedCharSequence orderedText = Text.literal((String)text).withStyle(style).getVisualOrderText();
        int totalWidth = (int)Math.ceil(Minecraft.getInstance().font.width(orderedText));
        float offset = IGui.VerticalAlignment.CENTER.getOffset(y, 10.0f);
        float xOffset = IGui.HorizontalAlignment.CENTER.getOffset(IGui.HorizontalAlignment.CENTER.getOffset(x, (float)totalWidth), 0.0f);
        float ratio = 1.0f;
        if (maxwidth != 0 && totalWidth > maxwidth) {
            // 【1.1.47】上游这里是 `ratio = maxwidth / totalWidth;` —— 两个 int 相除,
            // 走的是**整数除法**: maxwidth=1 / totalWidth=50 得到 0, 赋给 float 后
            // ratio 变成 0.0f, 随后的
            //     poseStack.scale(sx * ratio, ...)      -> 缩放为 0, 文字不渲染
            //     poseStack.scale(1.0f / sx / ratio, ...) -> **除以 0 -> Infinity/NaN**
            // 会把 NaN 写进 PoseStack 的矩阵, 污染同一批次后续所有渲染。
            //
            // 上游唯一传非零 maxwidth 的调用点是 RenderStationNameWall(传 1),
            // 而 STN_NAME_WALL 本身从未被注册(死方块), 所以这条路径不可达 ——
            // 但缺陷本身是真实的, 任何未来调用者传非零 maxwidth 都会踩到。
            //
            // 这里改成浮点除法, 并保证 ratio 落在 (0, 1] 区间, 从根上杜绝
            // 0 与 NaN/Inf 出现在缩放矩阵里。
            ratio = (float)maxwidth / (float)totalWidth;
            if (ratio <= 0.0f) {
                ratio = 1.0f;
            }
            else if (ratio > 1.0f) {
                ratio = 1.0f;
            }
        }
        // 【1.1.47】原本这里手工做「缩放 -> 画 -> 反向缩放」:
        //     poseStack.scale(sx * ratio, sy * ratio, sz);
        //     ... draw ...
        //     poseStack.scale(1.0f / sx / ratio, 1.0f / sy / ratio, 1.0f / sz);
        // 手写逆变换有两个隐患:
        //   1) sx/sy 为 0 时, 1.0f / 0 = Infinity -> 矩阵被写成 NaN/Inf,
        //      污染同一批次后续的所有渲染;
        //   2) 中间任何异常都会让逆变换不执行, 矩阵状态错乱。
        // 改用 pushPose/popPose 配对, 由 PoseStack 自己保证还原,
        // 既杜绝 Infinity/NaN, 也让异常安全。
        poseStack.pushPose();
        poseStack.scale(sx * ratio, sy * ratio, sz);
        UtilitiesClient.drawInBatch((net.minecraft.client.gui.Font) Minecraft.getInstance().font, (FormattedCharSequence)orderedText, (float)xOffset, (float)offset, (int)((r << 16) + (g << 8) + b), (boolean)false, (Matrix4f)poseStack.last().pose(), (MultiBufferSource)buffer, (int)0, (int)light);
        poseStack.popPose();
    }

    public static void drawText(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, float sx, float sy, float sz) {
        GraphicsHelper.drawText(Style.EMPTY, poseStack, buffer, dispatcher, text, x, y, r, g, b, light, sx, sy, sz, 0);
    }

    public static void drawText(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light) {
        GraphicsHelper.drawText(Style.EMPTY, poseStack, buffer, dispatcher, text, x, y, r, g, b, light, 1.0f, 1.0f, 1.0f, 0);
    }

    public static void drawTextFont(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light) {
        GraphicsHelper.drawText(Style.EMPTY.withFont(new ResourceLocation("shmetro", "shmetro")), poseStack, buffer, dispatcher, text, x, y, r, g, b, light, 1.0f, 1.0f, 1.0f, 0);
    }

    public static void drawTextFont(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, float sx, float sy, float sz) {
        GraphicsHelper.drawText(Style.EMPTY.withFont(new ResourceLocation("shmetro", "shmetro")), poseStack, buffer, dispatcher, text, x, y, r, g, b, light, sx * 0.7f, sy * 0.7f, sz * 0.7f, 0);
    }

    public static void drawText(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, float sx, float sy, float sz, int maxwidth) {
        GraphicsHelper.drawText(Style.EMPTY, poseStack, buffer, dispatcher, text, x, y, r, g, b, light, sx, sy, sz, maxwidth);
    }

    public static void drawText(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, int maxwidth) {
        GraphicsHelper.drawText(Style.EMPTY, poseStack, buffer, dispatcher, text, x, y, r, g, b, light, 1.0f, 1.0f, 1.0f, maxwidth);
    }

    public static void drawTextFont(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, int maxwidth) {
        GraphicsHelper.drawText(Style.EMPTY.withFont(new ResourceLocation("shmetro", "shmetro")), poseStack, buffer, dispatcher, text, x, y, r, g, b, light, 1.0f, 1.0f, 1.0f, maxwidth);
    }

    public static void drawTextFont(PoseStack poseStack, MultiBufferSource buffer, BlockEntityRenderDispatcher dispatcher, String text, float x, float y, int r, int g, int b, int light, float sx, float sy, float sz, int maxwidth) {
        GraphicsHelper.drawText(Style.EMPTY.withFont(new ResourceLocation("shmetro", "shmetro")), poseStack, buffer, dispatcher, text, x, y, r, g, b, light, sx * 0.7f, sy * 0.7f, sz * 0.7f, maxwidth);
    }

    public static void pasteImage(Graphics2D gph, ResourceLocation res, int x, int y) {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        InputStream stream = null;
        try {
            Optional<net.minecraft.server.packs.resources.Resource> resource = rm.getResource(res);
            if (!resource.isPresent()) {
                // 资源不存在: 贴图缺失是常态(玩家没装对应资源包/资源包版本不匹配),
                // 不该走异常路径 —— 异常会构造带完整栈追踪的对象, 而这里在
                // RMG 贴图生成时被反复调用,徒增 GC 压力。
                com.xhg78999.shmetro.Main.LOGGER.warn("SHM: 贴图资源缺失, 已跳过: " + res);
                return;
            }
            stream = Utilities.getInputStream(resource);
            if (stream == null) {
                // Utilities.getInputStream 在资源读不出来时返回 null(不抛异常),
                // ImageIO.read(null) 会抛 NPE。
                com.xhg78999.shmetro.Main.LOGGER.warn("SHM: 贴图资源流为空, 已跳过: " + res);
                return;
            }
            BufferedImage bfi = ImageIO.read(stream);
            if (bfi == null) {
                // ImageIO.read 在「数据不是合法图片」时返回 null 而不是抛异常,
                // 直接 drawImage(null, ...) 会 NPE。
                com.xhg78999.shmetro.Main.LOGGER.warn("SHM: 贴图资源无法解码, 已跳过: " + res);
                return;
            }
            gph.drawImage((Image)bfi, x, y, null);
        }
        catch (Exception e) {
            com.xhg78999.shmetro.Main.LOGGER.catching(e);
        }
        finally {
            // 【1.1.58 修文件句柄泄漏】原来这个 InputStream 从不关闭 ——
            // 上游反编译原件同错。pasteImage 在 RMG 贴图生成时被反复调用
            // (每个箭头/图标一次), 资源包里的贴图文件句柄一直不释放,
            // 长期运行会耗尽进程句柄。
            // ImageIO.read 已经把数据读进内存, 关流不影响任何可见行为。
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (Exception e) {
                    com.xhg78999.shmetro.Main.LOGGER.catching(e);
                }
            }
        }
    }

    public static void drawImageTextAlign(Graphics2D gph, Font font, String text, int x, int y, String alignment) {
        float length = GraphicsHelper.measureImageText(gph, font, text);
        int offset = switch (alignment) {
            case "left" -> x;
            case "right" -> (int)((float)x - length);
            case "middle" -> (int)((float)x - length / 2.0f);
            default -> throw new NoSuchElementException("Incorrect alignment");
        };
        gph.setFont(font);
        gph.drawString(text, offset, y);
    }

    public static int measureImageText(Graphics2D gph, Font font, String text) {
        return gph.getFontMetrics(font).stringWidth(text);
    }

    public static Color foreground(int color) {
        // 亮度选对比色。原实现两处错误(反编译原件同错):
        // 1) 蓝色系数 0.514 应为 0.114(标准亮度公式);
        // 2) RGB 是 0-255 量纲, 阈值却是 0.5 —— 结果除纯黑线色外恒返回 WHITE,
        //    亮线色(黄/白/浅灰)上白字不可读。按 YIQ 亮度中点 127 判定。
        Color c = new Color(color);
        return (double)c.getRed() * 0.299 + (double)c.getGreen() * 0.587 + (double)c.getBlue() * 0.114 < 127.5 ? Color.BLACK : Color.WHITE;
    }
}

