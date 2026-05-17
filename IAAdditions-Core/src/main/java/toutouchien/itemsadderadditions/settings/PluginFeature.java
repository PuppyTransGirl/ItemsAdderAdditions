package toutouchien.itemsadderadditions.settings;

import org.jspecify.annotations.NullMarked;

/**
 * Top-level feature flags from {@code config.yml}.
 *
 * <p>Keeping the keys in one enum avoids hard-coded feature paths being copied
 * across managers and listeners.</p>
 */
@NullMarked
public enum PluginFeature {
    CREATIVE_INVENTORY_INTEGRATION("creative_inventory_integration", true),
    CUSTOM_PAINTINGS("custom_paintings", true);

    private final String key;
    private final boolean defaultValue;

    PluginFeature(String key, boolean defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String key() {
        return key;
    }

    public boolean defaultValue() {
        return defaultValue;
    }

    public String path() {
        return "features." + key;
    }
}
