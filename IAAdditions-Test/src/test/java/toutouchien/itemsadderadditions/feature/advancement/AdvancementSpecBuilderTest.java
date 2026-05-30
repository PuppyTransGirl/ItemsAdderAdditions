package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import toutouchien.itemsadderadditions.feature.advancement.trigger.RuntimeTrigger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdvancementSpecBuilderTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static AdvancementDefinition makeRoot(String ns, String id) {
        var display = new AdvancementDisplayDefinition(
                ItemStack.of(Material.STONE), "Root", "desc", "task",
                "minecraft:textures/block/stone.png", false, false, false
        );
        return new AdvancementDefinition(
                new NamespacedKey(ns, id), null, display, List.of(),
                AdvancementRewardDefinition.EMPTY, CompletionActions.EMPTY
        );
    }

    private static AdvancementDefinition makeChild(String ns, String id, String parentId) {
        var display = new AdvancementDisplayDefinition(
                ItemStack.of(Material.DIAMOND), "Child", "desc", "task",
                null, true, true, false
        );
        var criterion = new AdvancementCriterionDefinition(
                "obtain", RuntimeTrigger.OBTAIN_ITEM, new AdvancementConditions.ObtainItem(List.of("ruby"), 1)
        );
        return new AdvancementDefinition(
                new NamespacedKey(ns, id),
                new NamespacedKey(ns, parentId),
                display, List.of(criterion),
                AdvancementRewardDefinition.EMPTY, CompletionActions.EMPTY
        );
    }

    @Test
    void root_hasAutoGrantRoot_true() {
        var spec = AdvancementSpecBuilder.build(makeRoot("ns", "root"));
        assertTrue(spec.autoGrantRoot());
        assertNull(spec.parent());
    }

    @Test
    void root_noUserCriteria_hasSyntheticCriterion() {
        var spec = AdvancementSpecBuilder.build(makeRoot("ns", "root"));
        assertEquals(1, spec.criteriaNames().size());
        assertEquals("root_trigger", spec.criteriaNames().getFirst());
    }

    @Test
    void child_parentKey_correct() {
        var spec = AdvancementSpecBuilder.build(makeChild("ns", "child", "root"));
        assertNotNull(spec.parent());
        assertEquals("ns", spec.parent().getNamespace());
        assertEquals("root", spec.parent().getKey());
    }

    @Test
    void child_criteriaNames_matchDefinition() {
        var spec = AdvancementSpecBuilder.build(makeChild("ns", "child", "root"));
        assertEquals(List.of("obtain"), spec.criteriaNames());
    }

    @Test
    void child_autoGrantRoot_false() {
        var spec = AdvancementSpecBuilder.build(makeChild("ns", "child", "root"));
        assertFalse(spec.autoGrantRoot());
    }

    @Test
    void rewards_mappedCorrectly() {
        var display = new AdvancementDisplayDefinition(
                ItemStack.of(Material.STONE), "T", "D", "task", null, true, true, false
        );
        var rewards = new AdvancementRewardDefinition(
                50, List.of("minecraft:chests/simple_dungeon"),
                List.of(new NamespacedKey("ns", "recipe1"))
        );
        var def = new AdvancementDefinition(
                new NamespacedKey("ns", "adv"), new NamespacedKey("ns", "root"),
                display, List.of(), rewards, CompletionActions.EMPTY
        );
        var spec = AdvancementSpecBuilder.build(def);
        assertEquals(50, spec.rewardExperience());
        assertEquals(List.of("minecraft:chests/simple_dungeon"), spec.rewardLoot());
        assertEquals(1, spec.rewardRecipes().size());
    }
}
