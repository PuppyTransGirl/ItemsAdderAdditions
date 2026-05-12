package toutouchien.itemsadderadditions.settings.migration;

import org.bukkit.configuration.file.FileConfiguration;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.util.Map;

/**
 * Shared helper for idempotent config migrations.
 *
 * <p>Each versioned converter simply declares its {@code DEFAULTS} map and calls
 * {@link #applyDefaults(Map)} - no boilerplate loops needed.
 *
 * <h3>Why idempotent?</h3>
 * Converters run on every plugin start. An entry is only written when the key is
 * completely absent from {@code config.yml}, so running a converter multiple times
 * is always safe.
 */
final class ConfigMigration {
    private ConfigMigration() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Writes any missing key-value pair from {@code defaults} into the plugin config
     * and saves if at least one key was added.
     *
     * @param defaults the key→default-value pairs to ensure are present
     */
    static void applyDefaults(Map<String, Object> defaults) {
        ItemsAdderAdditions plugin = ItemsAdderAdditions.instance();
        FileConfiguration config = plugin.getConfig();

        boolean dirty = false;
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!config.isSet(entry.getKey())) {
                config.set(entry.getKey(), entry.getValue());
                dirty = true;
            }
        }

        if (dirty) plugin.saveConfig();
    }
}
