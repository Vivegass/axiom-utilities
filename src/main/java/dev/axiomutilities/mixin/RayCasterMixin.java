package dev.axiomutilities.mixin;

import com.moulberry.axiom.RayCaster;
import dev.axiomutilities.PlacementOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every Axiom tool (cursor preview, click and drag strokes) ends up in this raycast overload. */
@Mixin(value = RayCaster.class, remap = false)
public abstract class RayCasterMixin {
    @Inject(
        method = "raycast(Lnet/minecraft/world/level/Level;Lorg/joml/Vector3d;Lorg/joml/Vector3d;ZZZ)Lcom/moulberry/axiom/RayCaster$RaycastResult;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private static void axiomutilities$offsetTarget(CallbackInfoReturnable<RayCaster.RaycastResult> cir) {
        RayCaster.RaycastResult original = cir.getReturnValue();
        RayCaster.RaycastResult shifted = PlacementOptions.apply(original);
        if (shifted != original) {
            cir.setReturnValue(shifted);
        }
    }
}
