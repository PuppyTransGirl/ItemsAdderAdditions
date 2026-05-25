package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbstractTriggerHandlerTest {
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

    private static final class TestHandler extends AbstractTriggerHandler {
        TestHandler(AdvancementRegistry registry) {
            super(registry);
        }

        NamespacedKey keyFor(AdvancementCriterionDefinition criterion) {
            return advancementKeyFor(criterion);
        }
    }
}
