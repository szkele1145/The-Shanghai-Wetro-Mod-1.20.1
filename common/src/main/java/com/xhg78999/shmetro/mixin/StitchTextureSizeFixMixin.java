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

    @ModifyVariable(method={"_getTexImage(IIIIJ)V"}, at=@At(value="HEAD"), ordinal=2, argsOnly=true, remap=false)
    private static int injectEnableGLCompressiongetImage(int i) {
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
}

