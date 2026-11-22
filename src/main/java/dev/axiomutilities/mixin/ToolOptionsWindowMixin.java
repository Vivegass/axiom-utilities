package dev.axiomutilities.mixin;

import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.windows.ToolOptionsWindow;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import dev.axiomutilities.PlacementOptions;
import dev.axiomutilities.presets.GlobalPresetsUI;
import imgui.moulberry92.ImGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the "Placement" options below the options of the active tool. */
@Mixin(value = ToolOptionsWindow.class, remap = false)
public abstract class ToolOptionsWindowMixin {
    /** Runs every editor frame (even when the window is closed): pipette hint next to the cursor. */
    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private static void axiomutilities$pipetteHint(CallbackInfo ci) {
        if (dev.axiomutilities.Eyedropper.isActive()) {
            ImGui.setTooltip("Pipette: click a block (Esc to cancel)");
        }
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/moulberry/axiom/tools/Tool;displayImguiOptions()V",
            shift = At.Shift.AFTER,
            remap = false
        ),
        remap = false
    )
    private static void axiomutilities$renderOption(CallbackInfo ci) {
        Tool tool = ToolManager.getCurrentTool();
        renderPlacement(tool);
        GlobalPresetsUI.render(tool);
    }

    private static void renderPlacement(Tool tool) {
        boolean offset = PlacementOptions.supportsOffset(tool);
        boolean airLayer = PlacementOptions.supportsAirLayer(tool);
        if (!offset && !airLayer) {
            return;
        }

        ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.placement_options"));

        if (offset) {
            boolean enabled = PlacementOptions.isOffsetEnabled(tool);
            if (ImGui.checkbox(AxiomI18n.get("axiom.tool.shape.offset_when_placing") + "##axiomutilities", enabled)) {
                PlacementOptions.setOffsetEnabled(tool, !enabled);
            }
            ImGui.setItemTooltip("Aim at the block next to the face you look at, instead of the block itself");
        }

        if (airLayer) {
            boolean enabled = PlacementOptions.isAirLayerEnabled(tool);
            if (ImGui.checkbox("Paint in air next to blocks##axiomutilities_air", enabled)) {
                PlacementOptions.setAirLayerEnabled(tool, !enabled);
            }
            ImGui.setItemTooltip("Paint into the air layer that touches the surface instead of recolouring the surface blocks");

            if (PlacementOptions.isAirLayerEnabled(tool)) {
                int[] thickness = {PlacementOptions.getThickness(tool)};
                if (ImGui.sliderInt("Layer thickness##axiomutilities_thickness", thickness, 1, PlacementOptions.MAX_THICKNESS)) {
                    PlacementOptions.setThickness(tool, thickness[0]);
                }
            }
        }
    }
}
