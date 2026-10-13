package com.xhg78999.shmetro.mixin;

import com.xhg78999.shmetro.resource.GraphicsCache;
import mtr.client.ClientCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientCache.class})
public class DynamicRefreshMixin {
    @Inject(method={"refreshDynamicResources()V"}, at={@At(value="RETURN")}, remap=false)
    private void injectRefresh(CallbackInfo ci) {
        GraphicsCache.CACHE.reloadDynamic();
    }
}

