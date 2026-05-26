package toutouchien.itemsadderadditions.settings.migration;

import java.util.Map;

/**
 * Migration from plugin version 1.0.8 -> 1.0.9.
 * Backfills the config key for the {@code custom_paintings} feature added in this version.
 */
public final class ConverterV108V109 {
    private static final Map<String, Object> DEFAULTS = Map.of(
            "actions.replace_item", true
    );

    private ConverterV108V109() {
    }

    public static void run() {
        ConfigMigration.applyDefaults(DEFAULTS);
    }
}
