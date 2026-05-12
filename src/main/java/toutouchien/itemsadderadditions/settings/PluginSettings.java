package toutouchien.itemsadderadditions.settings;

import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable snapshot of {@code config.yml}.
 *
 * <p>Subsystems consume this object instead of reading raw config paths. That
 * keeps defaults, validation, and reload behavior consistent.</p>
 */
@NullMarked
public record PluginSettings(
        Map<PluginFeature, Boolean> features,
        ToggleMap actions,
        ToggleMap behaviours,
        UpdateCheckerSettings updateChecker
) {
    public PluginSettings {
        features = Map.copyOf(features);
        actions = new ToggleMap(actions.values(), actions.defaultValue());
        behaviours = new ToggleMap(behaviours.values(), behaviours.defaultValue());
    }

    public static PluginSettings load(FileConfiguration config) {
        ConfigReader reader = new ConfigReader(config);
        EnumMap<PluginFeature, Boolean> features = readFeatures(reader);

        return new PluginSettings(
                features,
                reader.toggleSection("actions", true),
                reader.toggleSection("behaviours", true),
                readUpdateChecker(reader)
        );
    }

    private static EnumMap<PluginFeature, Boolean> readFeatures(ConfigReader reader) {
        EnumMap<PluginFeature, Boolean> features = new EnumMap<>(PluginFeature.class);
        for (PluginFeature feature : PluginFeature.values()) {
            features.put(feature, reader.bool(feature.path(), feature.defaultValue()));
        }
        return features;
    }

    private static UpdateCheckerSettings readUpdateChecker(ConfigReader reader) {
        return new UpdateCheckerSettings(
                reader.bool("update-checker.enabled", UpdateCheckerSettings.DEFAULT_ENABLED),
                reader.bool("update-checker.on-join", UpdateCheckerSettings.DEFAULT_NOTIFY_ON_JOIN)
        );
    }

    public boolean featureEnabled(PluginFeature feature) {
        return features.getOrDefault(feature, feature.defaultValue());
    }

    public boolean actionEnabled(String key) {
        return actions.enabled(key);
    }

    public boolean behaviourEnabled(String key) {
        return behaviours.enabled(key);
    }
}
