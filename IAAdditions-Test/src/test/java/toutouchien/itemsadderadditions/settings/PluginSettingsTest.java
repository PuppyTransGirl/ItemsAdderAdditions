package toutouchien.itemsadderadditions.settings;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginSettingsTest {
    private static PluginSettings loadFrom(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return PluginSettings.load(cfg);
    }

    @Test
    void load_emptyConfig_usesAllDefaults() {
        PluginSettings settings = loadFrom("");
        for (PluginFeature feature : PluginFeature.values()) {
            assertEquals(feature.defaultValue(), settings.featureEnabled(feature),
                    "Expected default for feature: " + feature);
        }
        // Default toggles: all actions/behaviours enabled
        assertTrue(settings.actionEnabled("any_action"));
        assertTrue(settings.behaviourEnabled("any_behaviour"));
    }

    @Test
    void load_featureDisabled_overridesDefault() {
        PluginSettings settings = loadFrom("""
                features:
                  creative_inventory_integration: false
                """);
        assertFalse(settings.featureEnabled(PluginFeature.CREATIVE_INVENTORY_INTEGRATION));
    }

    @Test
    void load_featureEnabled_explicitly() {
        PluginSettings settings = loadFrom("""
                features:
                  custom_paintings: true
                """);
        assertTrue(settings.featureEnabled(PluginFeature.CUSTOM_PAINTINGS));
    }

    @Test
    void load_actionDisabled_returnsDisabled() {
        PluginSettings settings = loadFrom("""
                actions:
                  title: false
                  ignite: true
                """);
        assertFalse(settings.actionEnabled("title"));
        assertTrue(settings.actionEnabled("ignite"));
        assertTrue(settings.actionEnabled("any_other")); // defaults to true
    }

    @Test
    void load_behaviourDisabled_returnsDisabled() {
        PluginSettings settings = loadFrom("""
                behaviours:
                  bed: false
                """);
        assertFalse(settings.behaviourEnabled("bed"));
        assertTrue(settings.behaviourEnabled("storage")); // default true
    }

    @Test
    void load_updateCheckerDefaults_bothTrue() {
        PluginSettings settings = loadFrom("");
        assertEquals(UpdateCheckerSettings.DEFAULT_ENABLED, settings.updateChecker().enabled());
        assertEquals(UpdateCheckerSettings.DEFAULT_NOTIFY_ON_JOIN, settings.updateChecker().notifyOnJoin());
    }

    @Test
    void load_updateCheckerDisabled() {
        PluginSettings settings = loadFrom("""
                update-checker:
                  enabled: false
                  on-join: false
                """);
        assertFalse(settings.updateChecker().enabled());
        assertFalse(settings.updateChecker().notifyOnJoin());
    }

    @Test
    void load_updateCheckerPartial_otherUsesDefault() {
        PluginSettings settings = loadFrom("""
                update-checker:
                  enabled: false
                """);
        assertFalse(settings.updateChecker().enabled());
        assertEquals(UpdateCheckerSettings.DEFAULT_NOTIFY_ON_JOIN, settings.updateChecker().notifyOnJoin());
    }

    @Test
    void featureEnabled_unknownFeature_usesDefault() {
        PluginSettings settings = loadFrom("");
        assertTrue(settings.featureEnabled(PluginFeature.CREATIVE_INVENTORY_INTEGRATION));
        assertTrue(settings.featureEnabled(PluginFeature.CUSTOM_PAINTINGS));
    }

    @Test
    void features_mapIsCopied_mutationSafe() {
        // Ensures the record's compact constructor copies the map
        PluginSettings settings = loadFrom("");
        assertNotNull(settings.features());
        assertThrows(UnsupportedOperationException.class,
                () -> settings.features().put(PluginFeature.CUSTOM_PAINTINGS, false));
    }
}
