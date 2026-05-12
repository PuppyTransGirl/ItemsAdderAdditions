package toutouchien.itemsadderadditions.settings.migration;

import java.util.Map;

/**
 * Migration from plugin version 1.0.6 → 1.0.7.
 * Backfills the config key for the {@code replace_biome} action added in this version.
 */
public final class ConverterV106V107 {
    private static final Map<String, Object> DEFAULTS = Map.of(
            "actions.replace_biome", true
    );

    private ConverterV106V107() {
    }

    public static void run() {
        ConfigMigration.applyDefaults(DEFAULTS);
    }
}
