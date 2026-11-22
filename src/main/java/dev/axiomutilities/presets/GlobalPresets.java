package dev.axiomutilities.presets;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.palette.ActiveBlockHistory;
import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import dev.axiomutilities.PlacementOptions;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Global presets: a snapshot of a tool's settings, its placement options, the tool mask and the active block,
 * stored one file per preset in config/axiom-utilities/presets.
 */
public final class GlobalPresets {
    public static final Path DIR = dev.axiomutilities.AxiomUtilities.CONFIG_DIR.resolve("presets");
    private static final Path VIEW_FILE = DIR.resolve(".view");

    private static List<Preset> presets = null;

    private GlobalPresets() {
    }

    /** Which parts of a preset to save or apply. */
    public record Parts(boolean tool, boolean placement, boolean mask, boolean block) {
        public boolean any() {
            return this.tool || this.placement || this.mask || this.block;
        }
    }

    /** One saved preset. {@code data} holds only the parts that were ticked when it was saved. */
    public static final class Preset {
        private final String name;
        private final Path path;
        private final CompoundTag data;
        private CustomBlockState iconBlock;
        private boolean iconResolved;

        Preset(String name, Path path, CompoundTag data) {
            this.name = name;
            this.path = path;
            this.data = data;
        }

        public String name() {
            return this.name;
        }

        public Path path() {
            return this.path;
        }

        public CompoundTag data() {
            return this.data;
        }

        public boolean hasTool() {
            return this.data.contains("Tool");
        }

        public boolean hasPlacement() {
            return this.data.contains("Placement") || this.data.getCompoundOrEmpty("Tool").contains("Addon");
        }

        public boolean hasMask() {
            return this.data.contains("Mask");
        }

        public boolean hasBlock() {
            return this.data.contains("ActiveBlock");
        }

        public String toolName() {
            if (this.hasTool()) {
                return this.data.getCompoundOrEmpty("Tool").getStringOr("Name", "?");
            }
            return this.data.getCompoundOrEmpty("Placement").getStringOr("Name", "?");
        }

        /** The tool this preset belongs to (from the brush or placement part), or null. */
        public Tool tool() {
            CompoundTag tag = this.hasTool() ? this.data.getCompoundOrEmpty("Tool") : this.data.getCompoundOrEmpty("Placement");
            return findTool(tag.getStringOr("Id", ""), tag.getStringOr("Class", ""));
        }

        /** Block shown on the preset's tile: first block of the brush, otherwise the saved active block. */
        public CustomBlockState iconBlock() {
            if (!this.iconResolved) {
                this.iconResolved = true;
                String block = findFirstBlock(this.data.getCompoundOrEmpty("Tool").getCompoundOrEmpty("Settings"));
                if (block == null) {
                    block = this.data.getStringOr("ActiveBlock", null);
                }
                if (block != null) {
                    try {
                        this.iconBlock = ServerCustomBlocks.deserialize(block);
                    } catch (Exception ignored) {
                        this.iconBlock = null;
                    }
                }
            }
            return this.iconBlock;
        }

        public String describe() {
            StringBuilder parts = new StringBuilder();
            if (this.hasTool()) {
                parts.append(this.toolName());
            }
            if (this.hasPlacement()) {
                parts.append(parts.isEmpty() ? "" : " + ").append("placement");
            }
            if (this.hasMask()) {
                parts.append(parts.isEmpty() ? "" : " + ").append("mask");
            }
            if (this.hasBlock()) {
                parts.append(parts.isEmpty() ? "" : " + ").append("block");
            }
            return parts.toString();
        }
    }

    // ---------------------------------------------------------------- list / delete

    public static List<Preset> list() {
        dev.axiomutilities.AxiomUtilities.migrateOldConfig();
        if (presets == null) {
            reload();
        }
        return presets;
    }

    public static void reload() {
        List<Preset> result = new ArrayList<>();
        if (Files.isDirectory(DIR)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIR, "*.nbt")) {
                for (Path file : stream) {
                    try {
                        CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                        String name = tag.getStringOr("Name", "");
                        if (!name.isEmpty()) {
                            result.add(new Preset(name, file, tag));
                        }
                    } catch (Exception e) {
                        log("Failed to read preset " + file + ": " + e);
                    }
                }
            } catch (Exception e) {
                log("Failed to list presets: " + e);
            }
        }
        result.sort(Comparator.comparing(Preset::name, String.CASE_INSENSITIVE_ORDER));
        presets = result;
    }

    public static Preset find(String name) {
        if (name == null) {
            return null;
        }
        for (Preset preset : list()) {
            if (preset.name().equalsIgnoreCase(name.trim())) {
                return preset;
            }
        }
        return null;
    }

    public static void delete(Preset preset) {
        try {
            Files.deleteIfExists(preset.path());
        } catch (Exception e) {
            log("Failed to delete preset " + preset.path() + ": " + e);
        }
        reload();
    }

    // ---------------------------------------------------------------- save

    /** Saves (or overwrites a preset with the same name). Returns an error message, or null on success. */
    public static String save(String rawName, Tool tool, Parts parts) {
        String name = rawName.trim();
        if (name.isEmpty()) {
            return "Enter a name";
        }
        if (!parts.any()) {
            return "Tick at least one thing to save";
        }

        CompoundTag root = new CompoundTag();
        root.putString("Name", name);

        if (parts.tool() && tool != null) {
            CompoundTag toolTag = toolIdentity(tool);
            CompoundTag settings = new CompoundTag();
            tool.writeSettings(settings);
            toolTag.put("Settings", settings);
            root.put("Tool", toolTag);
        }

        if (parts.placement() && tool != null) {
            CompoundTag placement = toolIdentity(tool);
            PlacementOptions.writeAddonSettings(tool, placement);
            root.put("Placement", placement);
        }

        if (parts.mask()) {
            CompoundTag maskTag = new CompoundTag();
            CompoundTag visual = new CompoundTag();
            ToolMaskWindow.writeSettings(visual);
            maskTag.put("Visual", visual);
            CompoundTag script = new CompoundTag();
            ToolMaskWindow.writeSettingsScript(script);
            maskTag.put("Script", script);
            maskTag.putBoolean("UseScript", MaskManager.useLuaScript);
            maskTag.putInt("Mode", MaskManager.mode[0]);
            maskTag.putBoolean("WindowOpen", EditorWindowType.TOOL_MASKS.isOpen());
            root.put("Mask", maskTag);
        }

        if (parts.block()) {
            try {
                CustomBlockState active = activeBlockHistory().getActive();
                if (active != null) {
                    root.putString("ActiveBlock", ServerCustomBlocks.serialize(active));
                }
            } catch (Exception e) {
                log("Failed to read active block: " + e);
            }
        }

        Preset existing = find(name);
        Path path = existing != null ? existing.path() : DIR.resolve(fileNameFor(name));
        try {
            Files.createDirectories(DIR);
            NbtIo.writeCompressed(root, path);
        } catch (Exception e) {
            log("Failed to save preset: " + e);
            return "Failed to save: " + e.getMessage();
        }
        reload();
        return null;
    }

    private static CompoundTag toolIdentity(Tool tool) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", tool.keybindId());
        tag.putString("Class", tool.getClass().getName());
        tag.putString("Name", tool.name());
        return tag;
    }

    // ---------------------------------------------------------------- apply

    /** Applies the ticked parts of a preset. Returns a warning message or null. */
    public static String apply(Preset preset, Parts parts) {
        CompoundTag data = preset.data();
        StringBuilder warnings = new StringBuilder();

        if (parts.tool() && preset.hasTool()) {
            CompoundTag toolTag = data.getCompoundOrEmpty("Tool");
            Tool tool = findTool(toolTag.getStringOr("Id", ""), toolTag.getStringOr("Class", ""));
            if (tool == null) {
                warnings.append("Tool '").append(toolTag.getStringOr("Name", "?")).append("' not found. ");
            } else {
                ToolManager.setTool(tool.getClass());
                tool.reset();
                tool.loadSettings(toolTag.getCompoundOrEmpty("Settings").copy());
            }
        }

        if (parts.placement() && preset.hasPlacement()) {
            // 1.2.0 stored placement inside the tool part as "Addon"
            CompoundTag placement = data.contains("Placement")
                ? data.getCompoundOrEmpty("Placement")
                : data.getCompoundOrEmpty("Tool").getCompoundOrEmpty("Addon");
            CompoundTag identity = data.contains("Placement") ? placement : data.getCompoundOrEmpty("Tool");
            Tool tool = findTool(identity.getStringOr("Id", ""), identity.getStringOr("Class", ""));
            if (tool != null) {
                PlacementOptions.loadAddonSettings(tool, placement);
            }
        }

        if (parts.mask() && preset.hasMask()) {
            CompoundTag maskTag = data.getCompoundOrEmpty("Mask");
            boolean useScript = maskTag.getBooleanOr("UseScript", false);
            ToolMaskWindow.loadSettings(maskTag.getCompoundOrEmpty("Visual"));
            ToolMaskWindow.loadSettingsScript(maskTag.getCompoundOrEmpty("Script"));
            MaskManager.useLuaScript = useScript;
            MaskManager.mode[0] = maskTag.getIntOr("Mode", 0);
            // The mask window decides between "Mask" and "Scripting" by its selected tab; switch it.
            setMaskTab(useScript ? 2 : 1);
            if (maskTag.getBooleanOr("WindowOpen", true)) {
                EditorWindowType.TOOL_MASKS.setOpen(true);
            }
        }

        if (parts.block() && preset.hasBlock()) {
            try {
                CustomBlockState state = ServerCustomBlocks.deserialize(data.getStringOr("ActiveBlock", ""));
                if (state == null) {
                    warnings.append("Block '").append(data.getStringOr("ActiveBlock", "")).append("' not found. ");
                } else {
                    activeBlockHistory().setActive(state);
                }
            } catch (Exception e) {
                warnings.append("Couldn't set the active block. ");
                log("Failed to set active block: " + e);
            }
        }

        return warnings.isEmpty() ? null : warnings.toString().trim();
    }

    // ---------------------------------------------------------------- view mode

    public static boolean loadGridView() {
        try {
            return Files.isRegularFile(VIEW_FILE) && Files.readString(VIEW_FILE, StandardCharsets.UTF_8).trim().equals("grid");
        } catch (Exception e) {
            return false;
        }
    }

    public static void saveGridView(boolean grid) {
        try {
            Files.createDirectories(DIR);
            Files.writeString(VIEW_FILE, grid ? "grid" : "list", StandardCharsets.UTF_8);
        } catch (Exception e) {
            log("Failed to save view mode: " + e);
        }
    }

    // ---------------------------------------------------------------- helpers

    static Tool findTool(String id, String className) {
        for (Tool tool : ToolManager.getTools()) {
            if (tool.getClass().getName().equals(className)) {
                return tool;
            }
        }
        for (Tool tool : ToolManager.getTools()) {
            if (!id.isEmpty() && tool.keybindId().equals(id)) {
                return tool;
            }
        }
        return null;
    }

    /** Depth-first search for the first string tag called "Block" (how Axiom tools store their block lists). */
    private static String findFirstBlock(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            if (compound.get("Block") instanceof StringTag(String value) && !value.isEmpty()) {
                return value;
            }
            for (String key : compound.keySet()) {
                String found = findFirstBlock(compound.get(key));
                if (found != null) {
                    return found;
                }
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                String found = findFirstBlock(list.get(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Field activeBlockHistoryField;
    private static Field switchMaskTabField;

    private static ActiveBlockHistory activeBlockHistory() throws ReflectiveOperationException {
        if (activeBlockHistoryField == null) {
            activeBlockHistoryField = EditorUI.class.getDeclaredField("activeBlockHistory");
            activeBlockHistoryField.setAccessible(true);
        }
        return (ActiveBlockHistory) activeBlockHistoryField.get(null);
    }

    private static void setMaskTab(int tab) {
        try {
            if (switchMaskTabField == null) {
                switchMaskTabField = ToolMaskWindow.class.getDeclaredField("switchMaskTab");
                switchMaskTabField.setAccessible(true);
            }
            switchMaskTabField.setInt(null, tab);
        } catch (Exception e) {
            log("Failed to switch mask tab: " + e);
        }
    }

    private static String fileNameFor(String name) {
        String base = name.replaceAll("[^\\p{L}\\p{N} _.-]", "_").trim();
        if (base.isEmpty() || base.startsWith(".")) {
            base = "preset";
        }
        if (base.length() > 48) {
            base = base.substring(0, 48);
        }
        String candidate = base + ".nbt";
        int i = 2;
        while (Files.exists(DIR.resolve(candidate))) {
            candidate = base + " (" + i++ + ").nbt";
        }
        return candidate;
    }

    private static void log(String message) {
        dev.axiomutilities.AxiomUtilities.LOG.warn(message);
    }
}
