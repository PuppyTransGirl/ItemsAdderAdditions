package toutouchien.itemsadderadditions.feature.itemmodel;

import com.google.gson.JsonObject;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemModelDefinitionParserTest {
    private final ItemModelDefinitionParser parser = new ItemModelDefinitionParser();

    @Test
    void oversizedInGui_isWrittenAtDefinitionRoot() {
        ItemModelDefinitionData definition = parser.parse(item("""
                items:
                  ruby:
                    item_model_definition:
                      oversized_in_gui: true
                      model:
                        type: model
                        model: my_pack:item/ruby
                """)).orElseThrow();

        JsonObject root = definition.json();
        assertTrue(root.get("oversized_in_gui").getAsBoolean());
        assertFalse(root.getAsJsonObject("model").has("oversized_in_gui"));
    }

    @Test
    void oversizedInGui_isOmittedByDefault() {
        ItemModelDefinitionData definition = parser.parse(item("""
                items:
                  ruby:
                    item_model_definition:
                      model:
                        type: model
                        model: my_pack:item/ruby
                """)).orElseThrow();

        assertFalse(definition.json().has("oversized_in_gui"));
    }

    private CustomStack item(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        CustomStack item = mock(CustomStack.class);
        when(item.getConfig()).thenReturn(config);
        when(item.getId()).thenReturn("ruby");
        when(item.getNamespace()).thenReturn("my_pack");
        when(item.getNamespacedID()).thenReturn("my_pack:ruby");
        return item;
    }
}
