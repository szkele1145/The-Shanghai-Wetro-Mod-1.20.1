package com.xhg78999.shmetro.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Deprecated
@Mixin(value={GlStateManager.class})
public class StitchTextureSizeFixMixin {
    @ModifyVariable(method={"_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"}, at=@At(value="HEAD"), ordinal=2, argsOnly=true, remap=false)
    private static int injectEnableGLCompressionImage(int i) {
        if (!GL.getCapabilities().GL_ARB_texture_compression) {
            return i;
        }
        return switch (i) {
            case 6408 -> 34030;
            case 6407 -> 34029;
            case 33319 -> 33318;
            case 6403 -> 33317;
            default -> i;
        };
    }

    @ModifyVariable(method={"_texSubImage2D(IIIIIIIIJ)V"}, at=@At(value="HEAD"), ordinal=2, argsOnly=true, remap=false)
    private static int injectEnableGLCompressionSubimage(int i) {
        if (!GL.getCapabilities().GL_ARB_texture_compression) {
            return i;
        }
        return switch (i) {
            case 6408 -> 34030;
            case 6407 -> 34029;
            case 33319 -> 33318;
            case 6403 -> 33317;
            default -> i;
        };
    }

    // 【1.1.56 删掉 _getTexImage 的注入】
    //
    // 原上游(1.19.4)写的是 method = {"_getTexImage (IIIIJ)V"} —— 方法名后面
    // 多了一个空格, Mixin 的 method 匹配是精确字符串匹配, 根本找不到目标
    // => 这个注入在上游一直是**静默失效**的。
    //
    // 移植到 1.20.1 时那个空格被顺手去掉了, 于是注入**真正生效**了 ——
    // 但目标参数是错的。1.20.1 的签名(字节码确认):
    //
    //   public static void _getTexImage(int, int, int, int, long)
    //       -> GL11.glGetTexImage(target, level, x, y, buffer)
    //
    // 第 3 个参数是 **x 坐标**, 压根没有「内部格式」这个参数
    // (glGetTexImage 是读像素, 不涉及纹理格式)。
    // ordinal=2 命中的就是 x 坐标, 把坐标当成格式常量送进 switch 完全是无意义操作。
    //
    // 实际危害为零(x 是小整数 0/1/2..., 永远落进 default 分支原样返回),
    // 但它既没有实现作者想要的语义(读纹理与压缩格式无关),
    // 又属于「移植引入的行为变化」(上游因方法名写错而根本没跑),
    // 所以整个注入删掉 —— 恢复上游的「不生效」状态, 且不留误导性的死代码。
}

