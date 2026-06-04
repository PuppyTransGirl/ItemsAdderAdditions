package toutouchien.itemsadderadditions.feature.itemmodel;

import com.google.gson.JsonObject;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemModelDefinitionNormalizerTest {
    private final ItemModelDefinitionNormalizer normalizer = new ItemModelDefinitionNormalizer();

    @Test
    void basicModel_shortTypeIsNormalized() {
        JsonObject model = normalizeModel("""
                type: model
                model: my_pack:item/ruby
                tints:
                  - type: constant
                    value: 16711680
                """);

        assertEquals("minecraft:model", model.get("type").getAsString());
        assertEquals("my_pack:item/ruby", model.get("model").getAsString());
        assertEquals("minecraft:constant", model.getAsJsonArray("tints")
                .get(0).getAsJsonObject().get("type").getAsString());
    }

    @Test
    void composite_nestedModelsAreNormalized() {
        JsonObject model = normalizeModel("""
                type: composite
                models:
                  - type: model
                    model: my_pack:item/base
                  - type: model
                    model: my_pack:item/overlay
                """);

        assertEquals("minecraft:composite", model.get("type").getAsString());
        assertEquals("minecraft:model", model.getAsJsonArray("models")
                .get(0).getAsJsonObject().get("type").getAsString());
    }

    @Test
    void select_propertyIsNormalized() {
        JsonObject model = normalizeModel("""
                type: select
                property: main_hand
                cases:
                  - when: left
                    model:
                      type: model
                      model: my_pack:item/left
                fallback:
                  type: model
                  model: my_pack:item/default
                """);

        assertEquals("minecraft:select", model.get("type").getAsString());
        assertEquals("minecraft:main_hand", model.get("property").getAsString());
    }

    @Test
    void special_nestedSpecialTypeIsNormalized() {
        JsonObject model = normalizeModel("""
                type: special
                base: minecraft:item/chest
                model:
                  type: chest
                  texture: my_pack:tiny_chest
                  openness: 0.0
                """);

        assertEquals("minecraft:special", model.get("type").getAsString());
        assertEquals("minecraft:chest", model.getAsJsonObject("model").get("type").getAsString());
    }

    @Test
    void rawRoot_isNotNormalized() {
        YamlConfiguration yaml = yaml("""
                model:
                  type: select
                  property: display_context
                  cases:
                    - when: gui
                      model:
                        type: model
                        model: my_pack:item/gui
                """);

        JsonObject root = normalizer.normalizeRawRoot(yaml, "my_pack:ruby").orElseThrow();
        assertEquals("select", root.getAsJsonObject("model").get("type").getAsString());
        assertEquals("display_context", root.getAsJsonObject("model").get("property").getAsString());
    }

    @Test
    void invalidRangeDispatchThreshold_isRejected() {
        assertTrue(normalizer.normalizeBuilderModel(yaml("""
                type: range_dispatch
                property: damage
                entries:
                  - threshold: nope
                    model:
                      type: model
                      model: my_pack:item/tool
                """), "my_pack:tool").isEmpty());
    }

    private JsonObject normalizeModel(String yaml) {
        return normalizer.normalizeBuilderModel(yaml(yaml), "my_pack:test").orElseThrow();
    }

    private YamlConfiguration yaml(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return config;
    }
}
