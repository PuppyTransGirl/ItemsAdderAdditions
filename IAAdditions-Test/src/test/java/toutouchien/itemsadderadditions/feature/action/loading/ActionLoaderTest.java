package toutouchien.itemsadderadditions.feature.action.loading;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.common.loading.AbstractItemsAdderItemLoader.ItemLoadContext;
import toutouchien.itemsadderadditions.common.registry.ExecutorRegistry;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.TriggerKey;
import toutouchien.itemsadderadditions.feature.action.TriggerType;
import toutouchien.itemsadderadditions.testsupport.RecordingActionExecutor;

import static org.junit.jupiter.api.Assertions.*;

class ActionLoaderTest {
    private ExecutorRegistry<ActionExecutor> registry;
    private ActionLoader loader;

    @BeforeEach
    void setUp() {
        ActionBindings.clear();
        registry = new ExecutorRegistry<>("Actions");
        registry.register(new RecordingActionExecutor());
        loader = new ActionLoader(registry);
    }

    @AfterEach
    void tearDown() {
        ActionBindings.clear();
    }

    private static FileConfiguration yaml(String body) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    private ItemLoadContext context(FileConfiguration config, ItemCategory category) {
        return new ItemLoadContext(config, "gem", "mypack:gem", category);
    }

    @Test
    void wildcardArgumentizedInteractBindsToInteractType() {
        FileConfiguration cfg = yaml("""
                items:
                  gem:
                    events:
                      interact:
                        recording: {}
                """);
        int count = loader.loadItem(context(cfg, ItemCategory.ITEM));

        assertEquals(1, count);
        assertTrue(ActionBindings.has("mypack:gem", TriggerType.ITEM_INTERACT));
    }

    @Test
    void argumentSpecificInteractBindsPerArgument() {
        FileConfiguration cfg = yaml("""
                items:
                  gem:
                    events:
                      interact:
                        right:
                          recording: {}
                        left_shift:
                          recording: {}
                """);
        int count = loader.loadItem(context(cfg, ItemCategory.ITEM));

        assertEquals(2, count);
        assertEquals(1, ActionBindings.get("mypack:gem", TriggerType.ITEM_INTERACT, "right").size());
        assertEquals(1, ActionBindings.get("mypack:gem", TriggerType.ITEM_INTERACT, "left_shift").size());
    }

    @Test
    void nonArgumentizedTriggerBinds() {
        FileConfiguration cfg = yaml("""
                items:
                  gem:
                    events:
                      block_break:
                        recording: {}
                """);
        int count = loader.loadItem(context(cfg, ItemCategory.ITEM));

        assertEquals(1, count);
        assertTrue(ActionBindings.has("mypack:gem", TriggerType.ITEM_BREAK_BLOCK));
    }

    @Test
    void unknownTriggerIsSkipped() {
        FileConfiguration cfg = yaml("""
                items:
                  gem:
                    events:
                      not_a_real_trigger:
                        recording: {}
                """);
        int count = loader.loadItem(context(cfg, ItemCategory.ITEM));

        assertEquals(0, count);
    }

    @Test
    void unknownActionKeyIsSkipped() {
        FileConfiguration cfg = yaml("""
                items:
                  gem:
                    events:
                      block_break:
                        no_such_action: {}
                """);
        int count = loader.loadItem(context(cfg, ItemCategory.ITEM));

        assertEquals(0, count);
    }

    @Test
    void missingEventsSectionReturnsZero() {
        FileConfiguration cfg = yaml("""
                items:
                  gem:
                    display_name: Gem
                """);
        assertEquals(0, loader.loadItem(context(cfg, ItemCategory.ITEM)));
    }

    @Test
    void blockCategoryAlsoParsesPlacedBlockSection() {
        FileConfiguration cfg = yaml("""
                items:
                  gem:
                    events:
                      break:
                        recording: {}
                      placed_block:
                        interact:
                          recording: {}
                """);
        int count = loader.loadItem(context(cfg, ItemCategory.BLOCK));

        assertEquals(2, count);
    }

    @Test
    void furnitureCategoryReadsPlacedFurnitureSection() {
        FileConfiguration cfg = yaml("""
                items:
                  gem:
                    events:
                      placed_furniture:
                        attack:
                          recording: {}
                """);
        int count = loader.loadItem(context(cfg, ItemCategory.FURNITURE));

        assertEquals(1, count);
        assertTrue(ActionBindings.has("mypack:gem", TriggerType.FURNITURE_ATTACK));
    }

    @Test
    void complexFurnitureInteractBindsToEntityId() {
        FileConfiguration cfg = yaml("""
                items:
                  gem:
                    behaviours:
                      complex_furniture:
                        entity: my_entity
                    events:
                      placed_furniture:
                        interact:
                          recording: {}
                """);
        int count = loader.loadItem(context(cfg, ItemCategory.COMPLEX_FURNITURE));

        assertEquals(1, count);
        assertTrue(ActionBindings.has("mypack:my_entity", TriggerType.COMPLEX_FURNITURE_INTERACT));
    }

    @Test
    void beforeLoadClearsExistingBindings() {
        ActionBindings.add("stale:id", TriggerKey.of(TriggerType.ITEM_INTERACT), new RecordingActionExecutor());
        assertTrue(ActionBindings.has("stale:id", TriggerType.ITEM_INTERACT));

        loader.load(java.util.List.of());

        assertFalse(ActionBindings.has("stale:id", TriggerType.ITEM_INTERACT));
    }
}
