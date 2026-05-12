package toutouchien.itemsadderadditions.settings;

import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * Immutable map of config-driven feature toggles with a defined fallback.
 */
@NullMarked
public record ToggleMap(Map<String, Boolean> values, boolean defaultValue) {
    public ToggleMap {
        values = Map.copyOf(values);
    }

    public boolean enabled(String key) {
        return values.getOrDefault(key, defaultValue);
    }
}
