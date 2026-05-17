package toutouchien.itemsadderadditions.common.loading;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFileCategoryTest {
    private static YamlConfiguration fromYaml(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    @Test
    void detectCampfireRecipes() {
        YamlConfiguration yaml = fromYaml("""
                recipes:
                  campfire_cooking:
                    some_recipe: {}
                """);
        assertTrue(ConfigFileCategory.CAMPFIRE_RECIPES.matches(yaml));
    }

    @Test
    void detectStonecutterRecipes() {
        YamlConfiguration yaml = fromYaml("""
                recipes:
                  stonecutter:
                    some_recipe: {}
                """);
        assertTrue(ConfigFileCategory.STONECUTTER_RECIPES.matches(yaml));
    }

    @Test
    void detectCraftingWithIaaCraftingTable() {
        YamlConfiguration yaml = fromYaml("""
                recipes:
                  iaa_crafting_table:
                    some_recipe: {}
                """);
        assertTrue(ConfigFileCategory.CRAFTING_RECIPES.matches(yaml));
    }

    @Test
    void detectCraftingWithIaaCrafting() {
        YamlConfiguration yaml = fromYaml("""
                recipes:
                  iaa_crafting:
                    some_recipe: {}
                """);
        assertTrue(ConfigFileCategory.CRAFTING_RECIPES.matches(yaml));
    }

    @Test
    void detectFurniturePopulatorsBlocksPopulators() {
        YamlConfiguration yaml = fromYaml("""
                blocks_populators:
                  some_populator: {}
                """);
        assertTrue(ConfigFileCategory.FURNITURE_POPULATORS.matches(yaml));
    }

    @Test
    void detectFurniturePopulatorsWorldsPopulatorsLegacy() {
        YamlConfiguration yaml = fromYaml("""
                worlds_populators:
                  some_populator: {}
                """);
        assertTrue(ConfigFileCategory.FURNITURE_POPULATORS.matches(yaml));
    }

    @Test
    void detectSurfaceDecorators() {
        YamlConfiguration yaml = fromYaml("""
                surface_decorators:
                  some_decorator: {}
                """);
        assertTrue(ConfigFileCategory.SURFACE_DECORATORS.matches(yaml));
    }

    @Test
    void detectPaintings() {
        YamlConfiguration yaml = fromYaml("""
                paintings:
                  some_painting: {}
                """);
        assertTrue(ConfigFileCategory.PAINTINGS.matches(yaml));
    }

    @Test
    void detectNoneForEmptyYaml() {
        YamlConfiguration yaml = new YamlConfiguration();
        EnumSet<ConfigFileCategory> result = ConfigFileCategory.detect(yaml);
        assertTrue(result.isEmpty());
    }

    @Test
    void detectNoneForUnrelatedContent() {
        YamlConfiguration yaml = fromYaml("""
                info:
                  name: my_item
                  display_name: My Item
                """);
        EnumSet<ConfigFileCategory> result = ConfigFileCategory.detect(yaml);
        assertTrue(result.isEmpty());
    }

    @Test
    void detectMultipleCategories() {
        YamlConfiguration yaml = fromYaml("""
                recipes:
                  campfire_cooking:
                    r: {}
                  stonecutter:
                    r: {}
                paintings:
                  p: {}
                """);
        EnumSet<ConfigFileCategory> result = ConfigFileCategory.detect(yaml);
        assertTrue(result.contains(ConfigFileCategory.CAMPFIRE_RECIPES));
        assertTrue(result.contains(ConfigFileCategory.STONECUTTER_RECIPES));
        assertTrue(result.contains(ConfigFileCategory.PAINTINGS));
        assertFalse(result.contains(ConfigFileCategory.CRAFTING_RECIPES));
    }

    @Test
    void recipesWithoutSubKeyDoesNotMatchCampfire() {
        YamlConfiguration yaml = fromYaml("""
                recipes:
                  something_else: {}
                """);
        assertFalse(ConfigFileCategory.CAMPFIRE_RECIPES.matches(yaml));
    }

    @Test
    void recipeSectionAbsentDoesNotMatchStonecutter() {
        YamlConfiguration yaml = fromYaml("""
                other:
                  stonecutter: {}
                """);
        assertFalse(ConfigFileCategory.STONECUTTER_RECIPES.matches(yaml));
    }
}
