package toutouchien.itemsadderadditions.settings.migration;

import java.util.Map;

/**
 * Migration from plugin version 1.0.5 → 1.0.6.
 * Backfills the config key for the {@code bed} behaviour added in this version.
 */
public final class ConverterV102V106 {
    private static final Map<String, Object> DEFAULTS = Map.of(
            "behaviours.bed", true
    );

    private ConverterV102V106() {
    }

    public static void run() {
        ConfigMigration.applyDefaults(DEFAULTS);
    }
}
