package toutouchien.itemsadderadditions.converter;

import java.util.Map;

/**
 * Migration from plugin version 1.0.1 → 1.0.2.
 * Backfills config keys for the actions and behaviours added in this version.
 */
public final class ConverterV101V102 {
    private static final Map<String, Object> DEFAULTS = Map.of(
            "actions.ignite", true,
            "actions.mythic_mobs_skill", true,
            "behaviours.storage", true
    );

    private ConverterV101V102() {
    }

    public static void run() {
        ConfigMigration.applyDefaults(DEFAULTS);
    }
}
