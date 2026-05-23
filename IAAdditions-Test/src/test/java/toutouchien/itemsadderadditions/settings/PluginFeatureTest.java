package toutouchien.itemsadderadditions.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginFeatureTest {
    @Test
    void creativeInventoryIntegrationKey() {
        assertEquals("creative_inventory_integration", PluginFeature.CREATIVE_INVENTORY_INTEGRATION.key());
    }

    @Test
    void creativeInventoryIntegrationDefaultIsTrue() {
        assertTrue(PluginFeature.CREATIVE_INVENTORY_INTEGRATION.defaultValue());
    }

    @Test
    void creativeInventoryIntegrationPath() {
        assertEquals("features.creative_inventory_integration", PluginFeature.CREATIVE_INVENTORY_INTEGRATION.path());
    }

    @Test
    void customPaintingsKey() {
        assertEquals("custom_paintings", PluginFeature.CUSTOM_PAINTINGS.key());
    }

    @Test
    void customPaintingsDefaultIsTrue() {
        assertTrue(PluginFeature.CUSTOM_PAINTINGS.defaultValue());
    }

    @Test
    void customPaintingsPath() {
        assertEquals("features.custom_paintings", PluginFeature.CUSTOM_PAINTINGS.path());
    }

    @Test
    void pathAlwaysStartsWithFeaturesPrefix() {
        for (PluginFeature feature : PluginFeature.values()) {
            assertTrue(feature.path().startsWith("features."),
                    feature.name() + " path should start with 'features.'");
        }
    }

    @Test
    void pathAlwaysContainsKey() {
        for (PluginFeature feature : PluginFeature.values()) {
            assertTrue(feature.path().contains(feature.key()),
                    feature.name() + " path should contain key '" + feature.key() + "'");
        }
    }
}
