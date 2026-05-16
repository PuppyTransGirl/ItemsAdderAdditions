package toutouchien.itemsadderadditions.settings.migration;

import java.util.Map;

/**
 * Migration from plugin version 1.0.7 → 1.0.8.
 * Backfills the config key for the {@code custom_paintings} feature added in this version.
 */
public final class ConverterV107V108 {
    private static final Map<String, Object> DEFAULTS = Map.of(
            "features.custom_paintings", true,

            "behaviours.text_display", true
    );

    private ConverterV107V108() {
    }

    public static void run() {
        ConfigMigration.applyDefaults(DEFAULTS);
    }
}
