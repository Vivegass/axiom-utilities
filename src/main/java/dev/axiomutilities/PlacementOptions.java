package dev.axiomutilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.gradient_painter.GradientPainterTool;
import com.moulberry.axiom.tools.modelling.ModellingTool;
import com.moulberry.axiom.tools.noise_painter.NoisePainterTool;
import com.moulberry.axiom.tools.painter.PainterTool;
import com.moulberry.axiom.tools.shape.ShapeTool;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tool settings of the addon:
 * <ul>
 *   <li>offset - move the tool's target point from the hit block to the neighbouring block on the hit face;</li>
 *   <li>airLayer - painter tools paint into air blocks touching the surface instead of into the surface blocks;</li>
 *   <li>thickness - how many blocks away from the surface the air layer reaches.</li>
 * </ul>
 */
public final class PlacementOptions {
    public static final int MAX_THICKNESS = 6;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = AxiomUtilities.CONFIG_DIR.resolve("placement.json");

    private static final Map<String, Boolean> OFFSET = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> AIR_LAYER = new ConcurrentHashMap<>();
    private static final Map<String, Integer> THICKNESS = new ConcurrentHashMap<>();

    static {
        load();
    }

    private PlacementOptions() {
    }

    // ---- which tools get which option ----

    /** Tools that already have their own "Offset target point" option are skipped. */
    public static boolean supportsOffset(Tool tool) {
        return tool != null && !(tool instanceof ModellingTool) && !(tool instanceof ShapeTool);
    }

    /** Tools that build their stroke from MaskManager.createSolidDestMask(). */
    public static boolean supportsAirLayer(Tool tool) {
        return tool instanceof PainterTool || tool instanceof NoisePainterTool || tool instanceof GradientPainterTool;
    }

    // ---- getters / setters ----

    public static boolean isOffsetEnabled(Tool tool) {
        return supportsOffset(tool) && OFFSET.getOrDefault(key(tool), Boolean.FALSE);
    }

    public static void setOffsetEnabled(Tool tool, boolean enabled) {
        OFFSET.put(key(tool), enabled);
        save();
    }

    public static boolean isAirLayerEnabled(Tool tool) {
        return supportsAirLayer(tool) && AIR_LAYER.getOrDefault(key(tool), Boolean.FALSE);
    }

    public static void setAirLayerEnabled(Tool tool, boolean enabled) {
        AIR_LAYER.put(key(tool), enabled);
        save();
    }

    public static int getThickness(Tool tool) {
        int value = THICKNESS.getOrDefault(key(tool), 1);
        return Math.max(1, Math.min(MAX_THICKNESS, value));
    }

    public static void setThickness(Tool tool, int thickness) {
        THICKNESS.put(key(tool), Math.max(1, Math.min(MAX_THICKNESS, thickness)));
        save();
    }

    /** Writes this addon's per-tool options into a preset. */
    public static void writeAddonSettings(Tool tool, net.minecraft.nbt.CompoundTag tag) {
        tag.putBoolean("Offset", OFFSET.getOrDefault(key(tool), Boolean.FALSE));
        tag.putBoolean("AirLayer", AIR_LAYER.getOrDefault(key(tool), Boolean.FALSE));
        tag.putInt("Thickness", getThickness(tool));
    }

    /** Restores this addon's per-tool options from a preset. */
    public static void loadAddonSettings(Tool tool, net.minecraft.nbt.CompoundTag tag) {
        OFFSET.put(key(tool), tag.getBooleanOr("Offset", false));
        AIR_LAYER.put(key(tool), tag.getBooleanOr("AirLayer", false));
        THICKNESS.put(key(tool), Math.max(1, Math.min(MAX_THICKNESS, tag.getIntOr("Thickness", 1))));
        save();
    }

    /** The tool currently in use, or null when no Axiom tool is active. */
    public static Tool activeTool() {
        try {
            return ToolManager.isToolActive() ? ToolManager.getCurrentTool() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Returns the offset result if the active tool has the option enabled, otherwise the same object. */
    public static RayCaster.RaycastResult apply(RayCaster.RaycastResult result) {
        if (result == null || result.isSelection() || result.direction() == null || OFFSET.isEmpty() || Eyedropper.isActive()) {
            return result;
        }
        if (!isOffsetEnabled(activeTool())) {
            return result;
        }
        return new RayCaster.RaycastResult(
            result.blockPos().relative(result.direction()), result.worldPos(), result.direction(), false);
    }

    private static String key(Tool tool) {
        return tool.getClass().getName();
    }

    // ---- config ----

    private static void load() {
        AxiomUtilities.migrateOldConfig();
        if (!Files.isRegularFile(CONFIG)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return;
            }
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("offset") || obj.has("airLayer") || obj.has("thickness")) {
                readBooleans(obj.get("offset"), OFFSET);
                readBooleans(obj.get("airLayer"), AIR_LAYER);
                if (obj.get("thickness") instanceof JsonObject t) {
                    t.entrySet().forEach(e -> {
                        if (e.getValue().isJsonPrimitive()) {
                            THICKNESS.put(e.getKey(), e.getValue().getAsInt());
                        }
                    });
                }
            } else {
                // 1.0.0 format: a flat map of tool class -> offset enabled
                readBooleans(obj, OFFSET);
            }
        } catch (Exception e) {
            AxiomUtilities.LOG.warn("Failed to read {}", CONFIG, e);
        }
    }

    private static void readBooleans(JsonElement element, Map<String, Boolean> into) {
        if (element instanceof JsonObject obj) {
            obj.entrySet().forEach(e -> {
                if (e.getValue().isJsonPrimitive()) {
                    into.put(e.getKey(), e.getValue().getAsBoolean());
                }
            });
        }
    }

    private static synchronized void save() {
        JsonObject root = new JsonObject();
        root.add("offset", GSON.toJsonTree(OFFSET));
        root.add("airLayer", GSON.toJsonTree(AIR_LAYER));
        root.add("thickness", GSON.toJsonTree(THICKNESS));
        try (Writer writer = Files.newBufferedWriter(CONFIG, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (Exception e) {
            AxiomUtilities.LOG.warn("Failed to save {}", CONFIG, e);
        }
    }
}
