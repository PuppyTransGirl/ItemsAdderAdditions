package toutouchien.itemsadderadditions.feature.recipe.crafting;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import toutouchien.itemsadderadditions.feature.recipe.RecipeActions;
import toutouchien.itemsadderadditions.nms.api.INmsCraftingRecipeHandler;

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
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static CraftingRecipeHandler freshHandler() {
        return new CraftingRecipeHandler(STUB_NMS);
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
}
