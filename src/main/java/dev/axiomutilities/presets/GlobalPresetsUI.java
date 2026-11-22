package dev.axiomutilities.presets;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.tools.Tool;
import imgui.moulberry92.ImDrawList;
import imgui.moulberry92.ImFont;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImString;
import java.util.List;

/** "Global presets" section drawn at the bottom of Axiom's Tool Options window. */
public final class GlobalPresetsUI {
    private static final ImString NAME = new ImString(64);

    // what to save
    private static boolean saveTool = true;
    private static boolean savePlacement = true;
    private static boolean saveMask = true;
    private static boolean saveBlock = true;

    // what to apply when a preset is picked
    private static boolean applyTool = true;
    private static boolean applyPlacement = true;
    private static boolean applyMask = true;
    private static boolean applyBlock = true;

    private static boolean onlyThisTool = false;
    private static Boolean gridView = null;
    private static String selected = null;
    private static String popupTarget = null;
    private static String message = null;
    private static boolean messageIsError = false;

    private GlobalPresetsUI() {
    }

    public static void render(Tool currentTool) {
        ImGui.separator();
        if (!ImGui.collapsingHeader("Global presets##au_global_presets")) {
            return;
        }
        if (gridView == null) {
            gridView = GlobalPresets.loadGridView();
        }
        ImGui.pushID("au_global_presets");
        try {
            renderToolbar();
            List<GlobalPresets.Preset> visible = visiblePresets(currentTool);
            if (gridView) {
                renderGrid(visible);
            } else {
                renderList(visible);
            }
            renderApplyCheckboxes();
            renderPresetPopups(currentTool);
            ImGui.separator();
            renderSave(currentTool);
            if (message != null) {
                if (messageIsError) {
                    ImGui.textColored(-11184641, message);
                } else {
                    ImGui.textDisabled(message);
                }
            }
        } finally {
            ImGui.popID();
        }
    }

    // ---------------------------------------------------------------- top row

    private static void renderToolbar() {
        if (ImGui.radioButton("List", !gridView)) {
            gridView = false;
            GlobalPresets.saveGridView(false);
        }
        ImGui.sameLine();
        if (ImGui.radioButton("Blocks", gridView)) {
            gridView = true;
            GlobalPresets.saveGridView(true);
        }
        ImGui.sameLine();
        if (ImGui.checkbox("This tool only", onlyThisTool)) {
            onlyThisTool = !onlyThisTool;
        }
        ImGui.sameLine();
        if (ImGui.button("Refresh")) {
            GlobalPresets.reload();
        }
    }

    private static List<GlobalPresets.Preset> visiblePresets(Tool currentTool) {
        List<GlobalPresets.Preset> all = GlobalPresets.list();
        if (!onlyThisTool || currentTool == null) {
            return all;
        }
        return all.stream()
            .filter(p -> !(p.hasTool() || p.hasPlacement()) || p.toolName().equals(currentTool.name()))
            .toList();
    }

    // ---------------------------------------------------------------- list view

    private static void renderList(List<GlobalPresets.Preset> presets) {
        GlobalPresets.Preset current = GlobalPresets.find(selected);
        String preview = current == null ? "Select preset..." : label(current);
        ImGui.setNextItemWidth(Math.max(80.0F, ImGui.getContentRegionAvailX()));
        if (ImGui.beginCombo("##preset", preview)) {
            for (GlobalPresets.Preset preset : presets) {
                if (ImGui.selectable(label(preset) + "##" + preset.path(), current == preset)) {
                    load(preset);
                }
            }
            if (presets.isEmpty()) {
                ImGui.textDisabled("No presets yet");
            }
            ImGui.endCombo();
        }

        if (current != null) {
            if (ImGui.button("Overwrite")) {
                openPopup("au_overwrite", current);
            }
            ImGui.sameLine();
            if (ImGui.button("Delete")) {
                openPopup("au_delete", current);
            }
        }
    }

    // ---------------------------------------------------------------- block grid view

    private static void renderGrid(List<GlobalPresets.Preset> presets) {
        if (presets.isEmpty()) {
            ImGui.textDisabled("No presets yet");
            return;
        }
        float scale = EditorUI.getUiScale();
        int size = Math.round(40 * scale);
        float spacing = 4 * scale;
        float available = ImGui.getContentRegionAvailX();
        int perRow = Math.max(1, (int) ((available + spacing) / (size + spacing)));

        float startX = ImGui.getCursorScreenPosX();
        float startY = ImGui.getCursorScreenPosY();
        ImDrawList drawList = ImGui.getWindowDrawList();
        int selectedColor = ImGui.getColorU32(23); // ImGuiCol_ButtonActive

        for (int i = 0; i < presets.size(); i++) {
            GlobalPresets.Preset preset = presets.get(i);
            float x = startX + (i % perRow) * (size + spacing);
            float y = startY + (i / perRow) * (size + spacing);
            ImGui.setCursorScreenPos(x, y);

            CustomBlockState block = preset.iconBlock();
            boolean clicked;
            if (block != null) {
                clicked = ImGuiHelper.blockStateButton(block, 7_000_000 + i, size);
            } else {
                clicked = ImGui.button("?##au_tile" + i, size, size);
            }
            boolean hovered = ImGui.isItemHovered();
            boolean rightClicked = ImGui.isItemClicked(1);

            drawToolBadge(drawList, preset, x, y, size, scale);
            if (preset.name().equalsIgnoreCase(selected == null ? "" : selected)) {
                drawList.addRect(x, y, x + size, y + size, selectedColor, 3.0F * scale, 0, 2.0F * scale);
            }

            if (clicked) {
                load(preset);
            }
            if (rightClicked) {
                popupTarget = preset.name();
                ImGui.openPopup("au_tile_menu");
            }
            if (hovered) {
                ImGui.beginTooltip();
                ImGui.textUnformatted(preset.name());
                ImGui.textDisabled(preset.describe());
                ImGui.textDisabled("Right click: overwrite / delete");
                ImGui.endTooltip();
            }
        }

        int rows = (presets.size() + perRow - 1) / perRow;
        ImGui.setCursorScreenPos(startX, startY + rows * (size + spacing));
        ImGui.dummy(1, 1);

        if (ImGui.beginPopup("au_tile_menu")) {
            GlobalPresets.Preset target = GlobalPresets.find(popupTarget);
            if (target == null) {
                ImGui.closeCurrentPopup();
            } else {
                ImGui.textDisabled(target.name());
                if (ImGui.selectable("Overwrite...")) {
                    pendingPopup = "au_overwrite";
                }
                if (ImGui.selectable("Delete...")) {
                    pendingPopup = "au_delete";
                }
            }
            ImGui.endPopup();
        }
    }

    /** Small tool icon in the bottom-right corner of a tile. */
    private static void drawToolBadge(ImDrawList drawList, GlobalPresets.Preset preset, float x, float y, int size, float scale) {
        Tool tool = preset.tool();
        String glyph;
        ImFont font;
        if (tool != null) {
            glyph = String.valueOf(tool.iconChar());
            font = EditorUI.icons;
        } else if (preset.hasMask()) {
            glyph = "M";
            font = null;
        } else {
            return;
        }
        int badge = Math.max(10, Math.round(size * 0.42F));
        float bx = x + size - badge - 1;
        float by = y + size - badge - 1;
        drawList.addRectFilled(bx - 1, by - 1, x + size - 1, y + size - 1, 0xC0000000, 3.0F * scale);
        if (font != null) {
            drawList.addText(font, badge, bx, by, 0xFFFFFFFF, glyph);
        } else {
            drawList.addText(bx + badge * 0.25F, by, 0xFFFFFFFF, glyph);
        }
    }

    // ---------------------------------------------------------------- shared

    private static void renderApplyCheckboxes() {
        ImGui.textDisabled("Load:");
        ImGui.sameLine();
        if (ImGui.checkbox("Brush##apply_tool", applyTool)) {
            applyTool = !applyTool;
        }
        ImGui.sameLine();
        if (ImGui.checkbox("Placement##apply_placement", applyPlacement)) {
            applyPlacement = !applyPlacement;
        }
        if (ImGui.checkbox("Mask##apply_mask", applyMask)) {
            applyMask = !applyMask;
        }
        ImGui.sameLine();
        if (ImGui.checkbox("Block##apply_block", applyBlock)) {
            applyBlock = !applyBlock;
        }
    }

    private static void load(GlobalPresets.Preset preset) {
        selected = preset.name();
        String warning = GlobalPresets.apply(preset, new GlobalPresets.Parts(applyTool, applyPlacement, applyMask, applyBlock));
        setMessage(warning == null ? "Loaded '" + preset.name() + "'" : warning, warning != null);
    }

    private static String pendingPopup = null;

    private static void openPopup(String id, GlobalPresets.Preset preset) {
        popupTarget = preset.name();
        pendingPopup = id;
    }

    private static void renderPresetPopups(Tool currentTool) {
        if (pendingPopup != null) {
            ImGui.openPopup(pendingPopup);
            pendingPopup = null;
        }
        GlobalPresets.Preset target = GlobalPresets.find(popupTarget);

        if (ImGui.beginPopup("au_delete")) {
            if (target == null) {
                ImGui.closeCurrentPopup();
            } else {
                ImGui.text("Delete '" + target.name() + "'?");
                if (ImGui.button("Delete##confirm")) {
                    GlobalPresets.delete(target);
                    setMessage("Deleted '" + target.name() + "'", false);
                    if (target.name().equalsIgnoreCase(selected == null ? "" : selected)) {
                        selected = null;
                    }
                    ImGui.closeCurrentPopup();
                }
                ImGui.sameLine();
                if (ImGui.button("Cancel##delete")) {
                    ImGui.closeCurrentPopup();
                }
            }
            ImGui.endPopup();
        }

        if (ImGui.beginPopup("au_overwrite")) {
            if (target == null) {
                ImGui.closeCurrentPopup();
            } else {
                ImGui.text("Overwrite '" + target.name() + "' with the current state?");
                renderSaveCheckboxes(currentTool, "ow");
                if (ImGui.button("Overwrite##confirm")) {
                    String error = GlobalPresets.save(target.name(), currentTool, saveParts());
                    setMessage(error == null ? "Saved '" + target.name() + "'" : error, error != null);
                    ImGui.closeCurrentPopup();
                }
                ImGui.sameLine();
                if (ImGui.button("Cancel##overwrite")) {
                    ImGui.closeCurrentPopup();
                }
            }
            ImGui.endPopup();
        }
    }

    private static void renderSave(Tool currentTool) {
        ImGui.textDisabled("Save current state as new preset:");
        ImGui.inputText("Name##au_name", NAME);
        renderSaveCheckboxes(currentTool, "new");
        if (ImGui.button("Save preset")) {
            String name = NAME.get().trim();
            GlobalPresets.Preset existing = GlobalPresets.find(name);
            String error = GlobalPresets.save(name, currentTool, saveParts());
            if (error == null) {
                selected = name;
                setMessage((existing != null ? "Overwrote '" : "Saved '") + name + "'", false);
            } else {
                setMessage(error, true);
            }
        }
    }

    private static GlobalPresets.Parts saveParts() {
        return new GlobalPresets.Parts(saveTool, savePlacement, saveMask, saveBlock);
    }

    private static void renderSaveCheckboxes(Tool currentTool, String id) {
        String toolLabel = currentTool == null ? "Brush settings" : "Brush settings (" + currentTool.name() + ")";
        if (ImGui.checkbox(toolLabel + "##save_tool_" + id, saveTool)) {
            saveTool = !saveTool;
        }
        ImGui.setItemTooltip("Everything in this tool's options: brush shape and size, blocks and percentages, noise, gradient...");
        if (ImGui.checkbox("Placement##save_placement_" + id, savePlacement)) {
            savePlacement = !savePlacement;
        }
        ImGui.setItemTooltip("The Placement options: Offset target point, Paint in air next to blocks, Layer thickness");
        if (ImGui.checkbox("Tool mask##save_mask_" + id, saveMask)) {
            saveMask = !saveMask;
        }
        ImGui.setItemTooltip("The mask (or mask script) from the Tool Masks window");
        if (ImGui.checkbox("Active block##save_block_" + id, saveBlock)) {
            saveBlock = !saveBlock;
        }
        ImGui.setItemTooltip("The currently selected block");
    }

    private static String label(GlobalPresets.Preset preset) {
        return preset.name() + "  [" + preset.describe() + "]";
    }

    private static void setMessage(String text, boolean error) {
        message = text;
        messageIsError = error;
    }
}
