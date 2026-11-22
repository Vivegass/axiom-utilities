package dev.axiomutilities.mixin;

import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import dev.axiomutilities.Eyedropper;
import imgui.moulberry92.ImGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a "Pick from world" (pipette) button to Axiom's Select Block dialog. */
@Mixin(value = SelectBlockWidget.class, remap = false)
public abstract class SelectBlockWidgetMixin {
    private static final String RENDER = "render(Ljava/lang/String;Lcom/moulberry/axiom/editor/BlockList;I)V";

    @Inject(method = RENDER, at = @At("HEAD"), remap = false)
    private void axiomutilities$deliverPicked(String label, BlockList blockList, int id, CallbackInfo ci) {
        Eyedropper.deliver((SelectBlockWidget) (Object) this, id);
    }

    @Inject(
        method = RENDER,
        at = @At(
            value = "FIELD",
            target = "Lcom/moulberry/axiom/editor/widgets/SelectBlockWidget;extraRender:Ljava/lang/Runnable;",
            ordinal = 0,
            remap = false
        ),
        remap = false
    )
    private void axiomutilities$pipetteButton(String label, BlockList blockList, int id, CallbackInfo ci) {
        if (ImGui.button("Pick from world (pipette)##axiomutilities_pipette", ImGui.getContentRegionAvailX(), 0.0F)) {
            Eyedropper.start((SelectBlockWidget) (Object) this, id);
            ImGui.closeCurrentPopup();
        }
        ImGui.setItemTooltip("Close this dialog, then click a block in the world to use it. Esc cancels.");
    }
}
