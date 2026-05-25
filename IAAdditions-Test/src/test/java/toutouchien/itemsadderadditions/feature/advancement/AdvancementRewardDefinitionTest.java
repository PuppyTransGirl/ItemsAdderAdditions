package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdvancementRewardDefinitionTest {
    @Test
    void empty_constant_isEmpty() {
        assertTrue(AdvancementRewardDefinition.EMPTY.isEmpty());
    }

    @Test
    void nonZeroExperience_isNotEmpty() {
        assertFalse(new AdvancementRewardDefinition(10, List.of(), List.of()).isEmpty());
    }

    @Test
    void withLoot_isNotEmpty() {
        assertFalse(new AdvancementRewardDefinition(0, List.of("minecraft:chests/dungeon"), List.of()).isEmpty());
    }

    @Test
    void withRecipes_isNotEmpty() {
        assertFalse(new AdvancementRewardDefinition(0, List.of(),
                List.of(new NamespacedKey("ns", "recipe"))).isEmpty());
    }

    @Test
    void fields_accessibleCorrectly() {
        var key = new NamespacedKey("ns", "r");
        var reward = new AdvancementRewardDefinition(42, List.of("loot_table"), List.of(key));
        assertEquals(42, reward.experience());
        assertEquals(List.of("loot_table"), reward.loot());
        assertEquals(List.of(key), reward.recipes());
    }
}
