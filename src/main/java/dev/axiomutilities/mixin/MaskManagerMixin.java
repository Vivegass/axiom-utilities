package dev.axiomutilities.mixin;

import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.tools.Tool;
import dev.axiomutilities.AirLayerMask;
import dev.axiomutilities.PlacementOptions;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Painter, Noise Painter and Gradient Painter ask for a "solid destination mask" when a stroke starts,
 * which only lets them touch blocks that have collision. When "Paint in air next to blocks" is on,
 * hand them a mask that selects the air layer around the surface instead (still honouring the user's mask).
 */
@Mixin(value = MaskManager.class, remap = false)
public abstract class MaskManagerMixin {
    @Inject(method = "createSolidDestMask", at = @At("HEAD"), cancellable = true, remap = false)
    private static void axiomutilities$airLayer(CallbackInfoReturnable<MaskElement> cir) {
        Tool tool = PlacementOptions.activeTool();
        if (!PlacementOptions.isAirLayerEnabled(tool) || Minecraft.getInstance().level == null) {
            return;
        }
        cir.setReturnValue(new AirLayerMask(MaskManager.getDestMask(), PlacementOptions.getThickness(tool), Minecraft.getInstance().level));
    }
}
