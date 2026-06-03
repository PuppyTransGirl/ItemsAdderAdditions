package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.common.namespace.CustomTagDefinition;
import toutouchien.itemsadderadditions.common.namespace.CustomTagRegistry;
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbstractTriggerHandlerTest {
    @AfterEach
    void clearTags() {
        NamespaceUtils.clearCustomTagRegistry();
    }

    @Test
    void advancementKeyForFindsOwningAdvancementByCriterionIdentity() {
        AdvancementCriterionDefinition criterion = new AdvancementCriterionDefinition(
                "mine_stone",
                RuntimeTrigger.BREAK_BLOCK,
                new AdvancementConditions.BreakBlock("minecraft:stone")
        );
        NamespacedKey key = new NamespacedKey("itemsadderadditions", "mine_stone");
        AdvancementRegistry registry = new AdvancementRegistry();
        registry.setAll(List.of(new AdvancementDefinition(key, null, null, List.of(criterion), null, null)));

        assertEquals(key, new TestHandler(registry).keyFor(criterion));
    }

    @Test
    void advancementKeyForThrowsWhenCriterionIsNotOwnedByAnyAdvancement() {
        AdvancementCriterionDefinition criterion = new AdvancementCriterionDefinition(
                "missing",
                RuntimeTrigger.BREAK_BLOCK,
                new AdvancementConditions.BreakBlock("minecraft:stone")
        );
        TestHandler handler = new TestHandler(new AdvancementRegistry());

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> handler.keyFor(criterion));
        assertTrue(error.getMessage().contains("missing"));
    }

    @Test
    void matchesRecipeAcceptsCustomRecipeTagsAndBareDirectKeys() {
        NamespaceUtils.setCustomTagRegistry(CustomTagRegistry.resolve(List.of(new CustomTagDefinition(
                "test", "sword_recipes", CustomTagType.RECIPE,
                List.of("ruby_sword_recipe", "minecraft:diamond_sword"), "test.yml"))));
        TestHandler handler = new TestHandler(new AdvancementRegistry());

        assertTrue(handler.recipeMatches("test:ruby_sword_recipe", "#test:sword_recipes"));
        assertTrue(handler.recipeMatches("minecraft:diamond_sword", "#test:sword_recipes"));
        assertTrue(handler.recipeMatches("minecraft:iron_sword", "iron_sword"));
        assertFalse(handler.recipeMatches("minecraft:stick", "#test:sword_recipes"));
    }

    private static final class TestHandler extends AbstractTriggerHandler {
        TestHandler(AdvancementRegistry registry) {
            super(registry);
        }

        NamespacedKey keyFor(AdvancementCriterionDefinition criterion) {
            return advancementKeyFor(criterion);
        }

        boolean recipeMatches(String actualRecipeKey, String expected) {
            return matchesRecipe(actualRecipeKey, expected);
        }
    }
}
