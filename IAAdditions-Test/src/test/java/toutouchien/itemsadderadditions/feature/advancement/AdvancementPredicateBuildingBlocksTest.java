package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.*;

class AdvancementPredicateBuildingBlocksTest {
    private static ServerMock server;
    private static WorldMock world;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static Object section(String yaml) {
        var cfg = new YamlConfiguration();
        try {
            cfg.loadFromString("root:\n" + yaml.indent(2));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg.get("root");
    }

    @Test
    void numericRanges_supportExactNumbersStringsAndMinMaxObjects() {
        assertTrue(IntRange.parseValue(5).matches(5));
        assertFalse(IntRange.parseValue(5).matches(6));
        assertTrue(DoubleRange.parseValue("2.5").matches(2.5D));

        IntRange range = IntRange.parse(section("""
                value:
                  min: 3
                  max: 7
                """), "value");
        assertTrue(range.matches(3));
        assertTrue(range.matches(7));
        assertFalse(range.matches(8));
    }

    @Test
    void distancePredicate_checksAbsoluteHorizontalAndAxisDistances() {
        DistancePredicate predicate = DistancePredicate.parse(section("""
                absolute:
                  min: 13
                  max: 13
                horizontal: 5
                x: 3
                y: 12
                z: 4
                """));

        assertNotNull(predicate);
        assertTrue(predicate.matches(
                new Location(world, 0, 64, 0),
                new Location(world, 3, 76, 4)
        ));
        assertFalse(predicate.matches(
                new Location(world, 0, 64, 0),
                new Location(world, 3, 75, 4)
        ));
    }

    @Test
    void itemPredicate_matchesItemsCountDurabilityAndSupportedComponents() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
        var meta = sword.getItemMeta();
        assertNotNull(meta);
        meta.setCustomModelData(123);
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(10);
        }
        sword.setItemMeta(meta);

        ItemPredicate predicate = ItemPredicate.parse("testns", section("""
                items: minecraft:diamond_sword
                count: 1
                durability:
                  min: 1550
                components:
                  minecraft:custom_model_data: 123
                """));

        assertTrue(predicate.matches(sword));

        ItemStack wrongModel = sword.clone();
        var wrongMeta = wrongModel.getItemMeta();
        assertNotNull(wrongMeta);
        wrongMeta.setCustomModelData(456);
        wrongModel.setItemMeta(wrongMeta);
        assertFalse(predicate.matches(wrongModel));
    }

    @Test
    void itemPredicate_failsClosedForUnsupportedComponentPredicates() {
        ItemPredicate predicate = ItemPredicate.parse("testns", section("""
                items: minecraft:diamond
                predicates:
                  minecraft:custom_data: {}
                """));

        assertFalse(predicate.matches(new ItemStack(Material.DIAMOND)));
    }

    @Test
    void slotBounds_parseNamedInventoryAndRangeForms() {
        assertArrayEquals(new int[]{0, 40}, AdvancementPredicateSupport.slotBounds("*"));
        assertArrayEquals(new int[]{2, 2}, AdvancementPredicateSupport.slotBounds("hotbar.2"));
        assertArrayEquals(new int[]{9, 9}, AdvancementPredicateSupport.slotBounds("inventory.0"));
        assertArrayEquals(new int[]{10, 13}, AdvancementPredicateSupport.slotBounds("inventory.1-4"));
        assertArrayEquals(new int[]{3, 6}, AdvancementPredicateSupport.slotBounds("container.3-6"));
        assertNull(AdvancementPredicateSupport.slotBounds("not_a_slot"));
    }
}
