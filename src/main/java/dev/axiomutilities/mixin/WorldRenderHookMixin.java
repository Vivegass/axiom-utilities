package dev.axiomutilities.mixin;

import com.moulberry.axiom.hooks.WorldRenderHook;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import dev.axiomutilities.Eyedropper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Outlines the block under the cursor while the pipette is active. */
@Mixin(value = WorldRenderHook.class, remap = false)
public abstract class WorldRenderHookMixin {
    @Inject(method = "renderHook", at = @At("TAIL"), remap = false)
    private static void axiomutilities$pipetteOutline(AxiomWorldRenderContext rc, CallbackInfo ci) {
        Eyedropper.render(rc);
    }
}
