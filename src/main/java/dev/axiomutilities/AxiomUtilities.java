package dev.axiomutilities;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared constants: logger and config locations. */
public final class AxiomUtilities {
    public static final String MOD_ID = "axiom_utilities";
    public static final Logger LOG = LoggerFactory.getLogger("Axiom Utilities");

    /** config/axiom-utilities */
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("axiom-utilities");

    private static boolean migrated = false;

    private AxiomUtilities() {
    }

    /**
     * Moves files written by the pre-release builds (named "Axiom Brush Offset") to the new locations,
     * once, without overwriting anything that already exists.
     */
    public static synchronized void migrateOldConfig() {
        if (migrated) {
            return;
        }
        migrated = true;
        Path configRoot = FabricLoader.getInstance().getConfigDir();
        try {
            Files.createDirectories(CONFIG_DIR);
            move(configRoot.resolve("axiom-brush-offset.json"), CONFIG_DIR.resolve("placement.json"));
            move(configRoot.resolve("axiom-global-presets"), CONFIG_DIR.resolve("presets"));
        } catch (Exception e) {
            LOG.warn("Failed to migrate old Axiom Brush Offset config", e);
        }
    }

    private static void move(Path from, Path to) throws Exception {
        if (Files.exists(from) && !Files.exists(to)) {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
            LOG.info("Moved {} to {}", from.getFileName(), to);
        }
    }
}
