package toutouchien.itemsadderadditions.settings;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed, validating facade over Bukkit's loosely-typed YAML API.
 */
@NullMarked
final class ConfigReader {
    private static final String LOG_TAG = "Config";

    private final FileConfiguration config;

    ConfigReader(FileConfiguration config) {
        this.config = config;
    }

    private static void warnInvalid(String path, String expected, @Nullable Object raw, Object defaultValue) {
        String actual = raw == null ? "null" : raw.getClass().getSimpleName();
        Log.warn(LOG_TAG, "Config key '{}' expected {}, got {} - using default '{}'.",
                path, expected, actual, defaultValue);
    }

    boolean bool(String path, boolean defaultValue) {
        if (!config.isSet(path)) {
            return defaultValue;
        }

        Object raw = config.get(path);
        if (raw instanceof Boolean value) {
            return value;
        }

        warnInvalid(path, "boolean", raw, defaultValue);
        return defaultValue;
    }

    int boundedInt(String path, int defaultValue, int minValue) {
        if (!config.isSet(path)) {
            return defaultValue;
        }

        Object raw = config.get(path);
        if (!(raw instanceof Number number)) {
            warnInvalid(path, "integer", raw, defaultValue);
            return defaultValue;
        }

        int value = number.intValue();
        if (value >= minValue) {
            return value;
        }

        Log.warn(LOG_TAG, "Config key '{}' value {} is below minimum {} - using {}.",
                path, value, minValue, minValue);
        return minValue;
    }

    String nonBlankString(String path, String defaultValue) {
        if (!config.isSet(path)) {
            return defaultValue;
        }

        Object raw = config.get(path);
        if (raw instanceof String value && !value.isBlank()) {
            return value;
        }

        warnInvalid(path, "non-empty string", raw, defaultValue);
        return defaultValue;
    }

    ToggleMap toggleSection(String path, boolean defaultValue) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return new ToggleMap(Collections.emptyMap(), defaultValue);
        }

        Map<String, Boolean> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            values.put(key, bool(path + "." + key, defaultValue));
        }
        return new ToggleMap(values, defaultValue);
    }
}
