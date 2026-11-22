package dev.axiomutilities.mixin;

import com.moulberry.axiom.UserAction;
import dev.axiomutilities.Eyedropper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While the pipette is active, world clicks pick a block instead of reaching the tool; Esc cancels. */
@Mixin(value = UserAction.class, remap = false)
public abstract class UserActionMixin {
    @Inject(method = "call", at = @At("HEAD"), cancellable = true, remap = false)
    private void axiomutilities$pipette(Object object, CallbackInfoReturnable<UserAction.ActionResult> cir) {
        if (!Eyedropper.isActive()) {
            return;
        }
        UserAction self = (UserAction) (Object) this;
        if (self == UserAction.LEFT_MOUSE || self == UserAction.RIGHT_MOUSE) {
            Eyedropper.pick();
            cir.setReturnValue(UserAction.ActionResult.USED_STOP);
        } else if (self == UserAction.ESCAPE) {
            Eyedropper.cancel();
            cir.setReturnValue(UserAction.ActionResult.USED_STOP);
        }
    }
}
