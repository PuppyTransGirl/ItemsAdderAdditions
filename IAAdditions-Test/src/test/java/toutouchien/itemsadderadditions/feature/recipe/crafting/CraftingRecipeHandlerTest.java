package toutouchien.itemsadderadditions.feature.recipe.crafting;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.recipe.RecipeActions;
import toutouchien.itemsadderadditions.nms.api.INmsCraftingRecipeHandler;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CraftingRecipeHandlerTest {
    private static final INmsCraftingRecipeHandler STUB_NMS = new INmsCraftingRecipeHandler() {
        @Override
        public void register(CraftingRecipeData data) {
        }

        @Override
        public void unregisterAll() {
        }
    };
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        NamespaceUtils.initVanillaCache();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static CraftingRecipeHandler freshHandler() {
        return new CraftingRecipeHandler(STUB_NMS);
    }

    private static YamlConfiguration yaml(String text) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(text);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return yaml;
    }

    @Test
    void loadedCount_initiallyZero() {
        assertEquals(0, freshHandler().loadedCount());
    }

    @Test
    void predicateRecipeByKey_unknownKey_returnsNull() {
        CraftingRecipeHandler handler = freshHandler();
        NamespacedKey key = new NamespacedKey("test", "unknown");
        assertNull(handler.predicateRecipeByKey(key));
    }

    @Test
    void actionsFor_unknownKey_returnsEmpty() {
        CraftingRecipeHandler handler = freshHandler();
        NamespacedKey key = new NamespacedKey("test", "unknown");
        assertSame(RecipeActions.EMPTY, handler.actionsFor(key));
    }

    @Test
    void unregisterAll_resetsLoadedCount() {
        CraftingRecipeHandler handler = freshHandler();
        handler.unregisterAll();
        assertEquals(0, handler.loadedCount());
    }

    @Test
    void unregisterAll_predicateRecipeByKeyStillReturnsNull() {
        CraftingRecipeHandler handler = freshHandler();
        handler.unregisterAll();
        assertNull(handler.predicateRecipeByKey(new NamespacedKey("test", "recipe")));
    }

    @Test
    void unregisterAll_actionsForStillReturnsEmpty() {
        CraftingRecipeHandler handler = freshHandler();
        handler.unregisterAll();
        assertSame(RecipeActions.EMPTY, handler.actionsFor(new NamespacedKey("test", "recipe")));
    }

    @Test
    void loadRegistersShapedPatternVariantsAndPredicateLookup() {
        List<CraftingRecipeData> registered = new ArrayList<>();
        CraftingRecipeHandler handler = new CraftingRecipeHandler(new INmsCraftingRecipeHandler() {
            @Override
            public void register(CraftingRecipeData data) {
                registered.add(data);
            }

            @Override
            public void unregisterAll() {
            }
        });
        YamlConfiguration yaml = yaml("""
                recipes:
                  tool:
                    result:
                      item: STONE
                      amount: 2
                    ingredients:
                      A:
                        item: DIRT
                        amount: 2
                        replacement: COBBLESTONE
                    pattern:
                      - "A "
                      - " Z"
                    pattern_alt:
                      - " A"
                      - "A "
                    permission: iaa.craft.tool
                """);

        handler.load("test", yaml.getConfigurationSection("recipes"));

        assertEquals(2, handler.loadedCount());
        assertEquals(2, registered.size());
        assertEquals(new NamespacedKey("test", "tool"), registered.getFirst().key());
        assertEquals(new NamespacedKey("test", "tool_v2"), registered.get(1).key());
        assertArrayEquals(new String[]{"A ", "  "}, registered.getFirst().pattern());
        assertArrayEquals(new String[]{" A", "A "}, registered.get(1).pattern());
        assertEquals(2, registered.getFirst().result().getAmount());
        assertEquals("iaa.craft.tool", registered.getFirst().permission());
        assertSame(registered.getFirst(), handler.predicateRecipeByKey(new NamespacedKey("test", "tool")));
        assertSame(registered.get(1), handler.predicateRecipeByKey(new NamespacedKey("test", "tool_v2")));
    }

    @Test
    void loadRegistersShapelessListRecipeWithoutPredicateIndexWhenNoPredicates() {
        List<CraftingRecipeData> registered = new ArrayList<>();
        CraftingRecipeHandler handler = new CraftingRecipeHandler(new INmsCraftingRecipeHandler() {
            @Override
            public void register(CraftingRecipeData data) {
                registered.add(data);
            }

            @Override
            public void unregisterAll() {
            }
        });
        YamlConfiguration yaml = yaml("""
                recipes:
                  mix:
                    shapeless: true
                    result:
                      item: STONE
                    ingredients:
                      - DIRT
                      - COBBLESTONE
                """);

        handler.load("test", yaml.getConfigurationSection("recipes"));

        assertEquals(1, handler.loadedCount());
        CraftingRecipeData data = registered.getFirst();
        assertFalse(data.shaped());
        assertNull(data.pattern());
        assertEquals(2, data.ingredients().size());
        assertNull(handler.predicateRecipeByKey(data.key()));
    }

    @Test
    void loadSkipsDisabledMalformedAndUnresolvableRecipes() {
        CraftingRecipeHandler handler = freshHandler();
        YamlConfiguration yaml = yaml("""
                recipes:
                  disabled:
                    enabled: false
                    result:
                      item: STONE
                    ingredients:
                      A: DIRT
                    pattern: ["A"]
                  missing_result:
                    ingredients:
                      A: DIRT
                    pattern: ["A"]
                  bad_result:
                    result:
                      item: no_such_material
                    ingredients:
                      A: DIRT
                    pattern: ["A"]
                  bad_ingredient_key:
                    result:
                      item: STONE
                    ingredients:
                      TOO_LONG: DIRT
                    pattern: ["T"]
                  no_pattern:
                    result:
                      item: STONE
                    ingredients:
                      A: DIRT
                """);

        handler.load("test", yaml.getConfigurationSection("recipes"));

        assertEquals(0, handler.loadedCount());
        assertTrue(handler.predicateRecipes().isEmpty());
    }

    @Test
    void registerFailureDoesNotIncrementLoadedCount() {
        CraftingRecipeHandler handler = new CraftingRecipeHandler(new INmsCraftingRecipeHandler() {
            @Override
            public void register(CraftingRecipeData data) {
                throw new IllegalStateException("boom");
            }

            @Override
            public void unregisterAll() {
            }
        });
        YamlConfiguration yaml = yaml("""
                recipes:
                  bad_register:
                    result:
                      item: STONE
                    ingredients:
                      A:
                        item: DIRT
                        amount: 2
                    pattern: ["A"]
                """);

        handler.load("test", yaml.getConfigurationSection("recipes"));

        assertEquals(0, handler.loadedCount());
        assertNull(handler.predicateRecipeByKey(new NamespacedKey("test", "bad_register")));
    }

    @Test
    void unregisterAllClearsRegisteredPredicateAndActions() {
        CraftingRecipeHandler handler = freshHandler();
        YamlConfiguration yaml = yaml("""
                recipes:
                  with_action:
                    result:
                      item: STONE
                    ingredients:
                      A:
                        item: DIRT
                        amount: 2
                    pattern: ["A"]
                    on_complete:
                      commands:
                        one:
                          command: say done
                          as_console: true
                """);

        handler.load("test", yaml.getConfigurationSection("recipes"));
        NamespacedKey key = new NamespacedKey("test", "with_action");
        assertNotNull(handler.predicateRecipeByKey(key));
        assertNotSame(RecipeActions.EMPTY, handler.actionsFor(key));

        handler.unregisterAll();

        assertEquals(0, handler.loadedCount());
        assertNull(handler.predicateRecipeByKey(key));
        assertSame(RecipeActions.EMPTY, handler.actionsFor(key));
    }
}
