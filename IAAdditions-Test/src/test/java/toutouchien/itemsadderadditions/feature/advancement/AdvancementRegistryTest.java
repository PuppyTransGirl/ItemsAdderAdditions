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

class AdvancementRegistryTest {
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
                new ItemStack(Material.STONE), "Root", "desc", "task",
                "minecraft:textures/block/stone.png", true, true, false
        );
        return new AdvancementDefinition(
                new NamespacedKey(ns, id), null, display, List.of(),
                AdvancementRewardDefinition.EMPTY, CompletionActions.EMPTY
        );
    }

    private static AdvancementDefinition makeChild(String ns, String id, String parentId, RuntimeTrigger trigger) {
        var display = new AdvancementDisplayDefinition(
                new ItemStack(Material.DIAMOND), "Child", "desc", "task", null, true, true, false
        );
        var criterion = new AdvancementCriterionDefinition("c1", trigger, AdvancementConditions.None.INSTANCE);
        return new AdvancementDefinition(
                new NamespacedKey(ns, id), new NamespacedKey(ns, parentId),
                display, List.of(criterion), AdvancementRewardDefinition.EMPTY, CompletionActions.EMPTY
        );
    }

    @Test
    void empty_initiallyEmpty() {
        var registry = new AdvancementRegistry();
        assertTrue(registry.all().isEmpty());
        assertTrue(registry.keys().isEmpty());
        assertTrue(registry.rootKeys().isEmpty());
    }

    @Test
    void setAll_populatesRegistry() {
        var registry = new AdvancementRegistry();
        var def = makeRoot("ns", "root");
        registry.setAll(List.of(def));
        assertEquals(1, registry.all().size());
        assertEquals(def, registry.get(new NamespacedKey("ns", "root")));
    }

    @Test
    void setAll_duplicateKey_lastWins() {
        var registry = new AdvancementRegistry();
        var def1 = makeRoot("ns", "root");
        var def2 = makeRoot("ns", "root");
        registry.setAll(List.of(def1, def2));
        assertEquals(1, registry.all().size());
        assertSame(def2, registry.get(new NamespacedKey("ns", "root")));
    }

    @Test
    void clear_removesAll() {
        var registry = new AdvancementRegistry();
        registry.setAll(List.of(makeRoot("ns", "root")));
        registry.clear();
        assertTrue(registry.all().isEmpty());
        assertNull(registry.get(new NamespacedKey("ns", "root")));
    }

    @Test
    void get_missingKey_returnsNull() {
        var registry = new AdvancementRegistry();
        assertNull(registry.get(new NamespacedKey("ns", "nonexistent")));
    }

    @Test
    void rootKeys_filterRootsOnly() {
        var registry = new AdvancementRegistry();
        var root = makeRoot("ns", "root");
        var child = makeChild("ns", "child", "root", RuntimeTrigger.IMPOSSIBLE);
        registry.setAll(List.of(root, child));
        var roots = registry.rootKeys();
        assertEquals(1, roots.size());
        assertEquals(new NamespacedKey("ns", "root"), roots.getFirst());
    }

    @Test
    void rootKeys_noRoots_returnsEmpty() {
        var registry = new AdvancementRegistry();
        registry.setAll(List.of(makeChild("ns", "child", "root", RuntimeTrigger.IMPOSSIBLE)));
        assertTrue(registry.rootKeys().isEmpty());
    }

    @Test
    void criteriaByTrigger_returnsMatchingOnly() {
        var registry = new AdvancementRegistry();
        var breakAdv = makeChild("ns", "break", "root", RuntimeTrigger.BREAK_BLOCK);
        var sleepAdv = makeChild("ns", "sleep", "root", RuntimeTrigger.SLEPT_IN_BED);
        registry.setAll(List.of(breakAdv, sleepAdv));
        var breakCriteria = registry.criteriaByTrigger(RuntimeTrigger.BREAK_BLOCK);
        assertEquals(1, breakCriteria.size());
        assertEquals(RuntimeTrigger.BREAK_BLOCK, breakCriteria.getFirst().trigger());
    }

    @Test
    void criteriaByTrigger_noMatch_returnsEmpty() {
        var registry = new AdvancementRegistry();
        registry.setAll(List.of(makeRoot("ns", "root")));
        assertTrue(registry.criteriaByTrigger(RuntimeTrigger.OBTAIN_ITEM).isEmpty());
    }

    @Test
    void keys_returnsAllKeys() {
        var registry = new AdvancementRegistry();
        var root = makeRoot("ns", "root");
        var child = makeChild("ns", "child", "root", RuntimeTrigger.IMPOSSIBLE);
        registry.setAll(List.of(root, child));
        assertEquals(2, registry.keys().size());
        assertTrue(registry.keys().contains(new NamespacedKey("ns", "root")));
        assertTrue(registry.keys().contains(new NamespacedKey("ns", "child")));
    }

    @Test
    void setAll_replacesPreviousContents() {
        var registry = new AdvancementRegistry();
        registry.setAll(List.of(makeRoot("ns", "first")));
        registry.setAll(List.of(makeRoot("ns", "second")));
        assertEquals(1, registry.all().size());
        assertNull(registry.get(new NamespacedKey("ns", "first")));
        assertNotNull(registry.get(new NamespacedKey("ns", "second")));
    }
}
