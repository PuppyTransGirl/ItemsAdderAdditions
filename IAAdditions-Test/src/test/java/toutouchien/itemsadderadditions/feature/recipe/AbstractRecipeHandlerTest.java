package toutouchien.itemsadderadditions.feature.recipe;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractRecipeHandlerTest {
    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @Test
    void loadedCount_initiallyZero() {
        assertEquals(0, new StubHandler().loadedCount());
    }

    @Test
    void incrementCount_increasesLoadedCount() {
        StubHandler h = new StubHandler();
        h.incrementCount();
        h.incrementCount();
        assertEquals(2, h.loadedCount());
    }

    @Test
    void resetCount_setsToZero() {
        StubHandler h = new StubHandler();
        h.incrementCount();
        h.incrementCount();
        h.resetCount();
        assertEquals(0, h.loadedCount());
    }

    @Test
    void load_nullSection_isNoOp() {
        StubHandler h = new StubHandler();
        h.load("ns", null);
        assertEquals(0, h.loadEntryCalls);
        assertEquals(0, h.loadedCount());
    }

    @Test
    void load_disabledEntry_isSkipped() {
        StubHandler h = new StubHandler();
        YamlConfiguration cfg = yamlOf("my_recipe:\n  enabled: false\n  ingredient:\n    item: stone\n");
        h.load("ns", cfg.getConfigurationSection(""));
        assertEquals(0, h.loadEntryCalls);
    }

    @Test
    void load_enabledEntry_callsLoadEntry() {
        StubHandler h = new StubHandler();
        YamlConfiguration cfg = yamlOf("my_recipe:\n  enabled: true\n");
        h.load("ns", cfg);
        assertEquals(1, h.loadEntryCalls);
    }

    @Test
    void load_noEnabledKey_defaultsToEnabled() {
        StubHandler h = new StubHandler();
        YamlConfiguration cfg = yamlOf("my_recipe:\n  some_key: value\n");
        h.load("ns", cfg);
        assertEquals(1, h.loadEntryCalls);
    }

    @Test
    void load_multipleEntries_allEnabled_callsLoadEntryForEach() {
        StubHandler h = new StubHandler();
        YamlConfiguration cfg = yamlOf("r1:\n  enabled: true\nr2:\n  enabled: true\nr3:\n  enabled: false\n");
        h.load("ns", cfg);
        assertEquals(2, h.loadEntryCalls);
    }

    @Test
    void load_multipleEntries_counterReflectsRegistered() {
        StubHandler h = new StubHandler();
        YamlConfiguration cfg = yamlOf("r1:\n  x: 1\nr2:\n  x: 2\n");
        h.load("ns", cfg);
        assertEquals(2, h.loadedCount());
    }

    @Test
    void unregisterAll_clearsRegistered() {
        StubHandler h = new StubHandler();
        h.registered.add("ns:test");
        h.unregisterAll();
        assertTrue(h.registered.isEmpty());
    }

    /**
     * Minimal stub that tracks calls and never touches NMS.
     */
    @NullMarked
    private static final class StubHandler extends AbstractRecipeHandler {
        final List<String> registered = new ArrayList<>();
        int loadEntryCalls = 0;

        StubHandler() {
            super("TestRecipe");
        }

        @Override
        protected void registerRecipe(String namespace, String recipeId,
                                      ConfigurationSection entry, ItemStack ingredient, ItemStack result) {
            registered.add(namespace + ":" + recipeId);
            incrementCount();
        }

        @Override
        protected void loadEntry(String namespace, String recipeId, ConfigurationSection entry) {
            loadEntryCalls++;
            // Count without resolving items (avoids NamespaceUtils)
            incrementCount();
        }

        @Override
        public void unregisterAll() {
            registered.clear();
        }
    }
}
