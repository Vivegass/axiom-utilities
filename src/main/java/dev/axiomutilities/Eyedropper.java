package dev.axiomutilities;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.tools.Tool;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pipette for Axiom's "Select Block" dialog: the dialog closes, the next click on a block in the world
 * picks that block, and it is handed back to the exact dialog (widget + row id) that started picking.
 */
public final class Eyedropper {
    private static SelectBlockWidget targetWidget;
    private static int targetId;
    private static boolean active;

    private static SelectBlockWidget resultWidget;
    private static int resultId;
    private static CustomBlockState result;

    private static Field resultStateField;

    private Eyedropper() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void start(SelectBlockWidget widget, int id) {
        targetWidget = widget;
        targetId = id;
        active = true;
        result = null;
    }

    public static void cancel() {
        active = false;
        targetWidget = null;
    }

    /** Raycast without the addon's target offset, so the pipette always picks the block you look at. */
    public static RayCaster.RaycastResult raycast() {
        return Tool.raycastBlock(false, true, false);
    }

    /** Called on a world click while picking. */
    public static void pick() {
        RayCaster.RaycastResult hit = raycast();
        if (hit == null || Minecraft.getInstance().level == null) {
            return; // clicked the sky: keep picking
        }
        BlockState state = Minecraft.getInstance().level.getBlockState(hit.blockPos());
        CustomBlockState custom = ServerCustomBlocks.getCustomOrVanillaStateFor(state);
        if (custom == null) {
            return;
        }
        resultWidget = targetWidget;
        resultId = targetId;
        result = custom;
        cancel();
    }

    /** Called at the start of SelectBlockWidget.render(label, list, id): delivers a picked block to its row. */
    public static void deliver(SelectBlockWidget widget, int id) {
        if (result == null || widget != resultWidget || id != resultId) {
            return;
        }
        try {
            if (resultStateField == null) {
                resultStateField = SelectBlockWidget.class.getDeclaredField("resultState");
                resultStateField.setAccessible(true);
            }
            resultStateField.set(widget, result);
        } catch (ReflectiveOperationException e) {
            AxiomUtilities.LOG.warn("Failed to deliver picked block", e);
        }
        result = null;
        resultWidget = null;
    }

    /** Outline of the block under the cursor while picking. */
    public static void render(AxiomWorldRenderContext rc) {
        if (!active) {
            return;
        }
        RayCaster.RaycastResult hit = raycast();
        if (hit != null) {
            Tool.renderRaycastOverlay(rc, hit.blockPos());
        }
    }
}
