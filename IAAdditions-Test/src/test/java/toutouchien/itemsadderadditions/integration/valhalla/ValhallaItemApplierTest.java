package toutouchien.itemsadderadditions.integration.valhalla;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ValhallaItemApplierTest {
    private static final NamespacedKey KEY_ACTUAL_STATS =
            NamespacedKey.fromString("valhallammo:actual_stats");
    private static final NamespacedKey KEY_DEFAULT_STATS =
            NamespacedKey.fromString("valhallammo:default_stats");
    private static final NamespacedKey KEY_EQUIPMENT_CLASS =
            NamespacedKey.fromString("valhallammo:equipment_class");
    private static final NamespacedKey KEY_ITEM_FLAGS =
            NamespacedKey.fromString("valhallammo:item_flags");
    private static final NamespacedKey KEY_TRINKET_ID =
            NamespacedKey.fromString("valhallatrinkets:trinket_id");
    private static final NamespacedKey KEY_TRINKET_UNIQUE_ID =
            NamespacedKey.fromString("valhallatrinkets:trinket_unique_id");
    private static final NamespacedKey KEY_TRINKET_UNIQUE =
            NamespacedKey.fromString("valhallatrinkets:unique");

    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ItemStack sword() {
        return ItemStack.of(Material.DIAMOND_SWORD);
    }

    private static ItemMeta meta(ItemStack stack) {
        return Objects.requireNonNull(stack.getItemMeta());
    }

    @Test
    void appliesActualStats() {
        ValhallaStatEntry entry = new ValhallaStatEntry("CRIT_DAMAGE", 1000.0, "ADD_NUMBER", true);
        ValhallaItemData data = new ValhallaItemData(
                List.of(entry), List.of(), null, List.of(), null);

        ItemStack result = ValhallaItemApplier.apply(sword(), data);

        String pdc = meta(result).getPersistentDataContainer()
                .get(KEY_ACTUAL_STATS, PersistentDataType.STRING);
        assertEquals("CRIT_DAMAGE:1000.0:ADD_NUMBER:true", pdc);
    }

    @Test
    void appliesDefaultStats() {
        ValhallaStatEntry entry = new ValhallaStatEntry("JUMPS", 3.0, "ADD_NUMBER", false);
        ValhallaItemData data = new ValhallaItemData(
                List.of(), List.of(entry), null, List.of(), null);

        ItemStack result = ValhallaItemApplier.apply(sword(), data);

        String pdc = meta(result).getPersistentDataContainer()
                .get(KEY_DEFAULT_STATS, PersistentDataType.STRING);
        assertEquals("JUMPS:3.0:ADD_NUMBER:false", pdc);
    }

    @Test
    void appliesBothActualAndDefaultStats() {
        ValhallaStatEntry entry = new ValhallaStatEntry("GENERIC_MOVEMENT_SPEED", 0.3, "ADD_SCALAR", false);
        ValhallaItemData data = new ValhallaItemData(
                List.of(entry), List.of(entry), null, List.of(), null);

        ItemStack result = ValhallaItemApplier.apply(sword(), data);
        var pdc = meta(result).getPersistentDataContainer();

        assertEquals("GENERIC_MOVEMENT_SPEED:0.3:ADD_SCALAR:false",
                pdc.get(KEY_ACTUAL_STATS, PersistentDataType.STRING));
        assertEquals("GENERIC_MOVEMENT_SPEED:0.3:ADD_SCALAR:false",
                pdc.get(KEY_DEFAULT_STATS, PersistentDataType.STRING));
    }

    @Test
    void multipleStatsSerializedWithSemicolon() {
        List<ValhallaStatEntry> stats = List.of(
                new ValhallaStatEntry("GENERIC_MOVEMENT_SPEED", 0.3, "ADD_SCALAR", false),
                new ValhallaStatEntry("JUMPS", 3.0, "ADD_NUMBER", false),
                new ValhallaStatEntry("JUMP_HEIGHT", 1.0, "ADD_NUMBER", false)
        );
        ValhallaItemData data = new ValhallaItemData(stats, stats, null, List.of(), null);

        ItemStack result = ValhallaItemApplier.apply(sword(), data);
        String pdc = meta(result).getPersistentDataContainer()
                .get(KEY_ACTUAL_STATS, PersistentDataType.STRING);

        assertEquals("GENERIC_MOVEMENT_SPEED:0.3:ADD_SCALAR:false;JUMPS:3.0:ADD_NUMBER:false;JUMP_HEIGHT:1.0:ADD_NUMBER:false",
                pdc);
    }

    @Test
    void appliesEquipmentClass() {
        ValhallaItemData data = new ValhallaItemData(List.of(), List.of(), "TRINKET", List.of(), null);

        ItemStack result = ValhallaItemApplier.apply(sword(), data);
        String pdc = meta(result).getPersistentDataContainer()
                .get(KEY_EQUIPMENT_CLASS, PersistentDataType.STRING);

        assertEquals("TRINKET", pdc);
    }

    @Test
    void appliesSingleItemFlag() {
        ValhallaItemData data = new ValhallaItemData(
                List.of(), List.of(), null, List.of("DISPLAY_ATTRIBUTES"), null);

        ItemStack result = ValhallaItemApplier.apply(sword(), data);
        String pdc = meta(result).getPersistentDataContainer()
                .get(KEY_ITEM_FLAGS, PersistentDataType.STRING);

        assertEquals("DISPLAY_ATTRIBUTES", pdc);
    }

    @Test
    void appliesMultipleItemFlagsJoinedBySemicolon() {
        ValhallaItemData data = new ValhallaItemData(
                List.of(), List.of(), null, List.of("DISPLAY_ATTRIBUTES", "HIDE_TAGS"), null);

        ItemStack result = ValhallaItemApplier.apply(sword(), data);
        String pdc = meta(result).getPersistentDataContainer()
                .get(KEY_ITEM_FLAGS, PersistentDataType.STRING);

        assertEquals("DISPLAY_ATTRIBUTES;HIDE_TAGS", pdc);
    }

    @Test
    void appliesTrinketId() {
        ValhallaTrinketData trinkets = new ValhallaTrinketData(7, null, null);
        ValhallaItemData data = new ValhallaItemData(List.of(), List.of(), null, List.of(), trinkets);

        ItemStack result = ValhallaItemApplier.apply(sword(), data);
        Integer pdc = meta(result).getPersistentDataContainer()
                .get(KEY_TRINKET_ID, PersistentDataType.INTEGER);

        assertEquals(7, pdc);
    }

    @Test
    void appliesTrinketUniqueId() {
        ValhallaTrinketData trinkets = new ValhallaTrinketData(null, 529, null);
        ValhallaItemData data = new ValhallaItemData(List.of(), List.of(), null, List.of(), trinkets);

        ItemStack result = ValhallaItemApplier.apply(sword(), data);
        Integer pdc = meta(result).getPersistentDataContainer()
                .get(KEY_TRINKET_UNIQUE_ID, PersistentDataType.INTEGER);

        assertEquals(529, pdc);
    }

    @Test
    void appliesTrinketUniqueTrue() {
        ValhallaTrinketData trinkets = new ValhallaTrinketData(null, null, true);
        ValhallaItemData data = new ValhallaItemData(List.of(), List.of(), null, List.of(), trinkets);

        ItemStack result = ValhallaItemApplier.apply(sword(), data);
        Byte pdc = meta(result).getPersistentDataContainer()
                .get(KEY_TRINKET_UNIQUE, PersistentDataType.BYTE);

        assertEquals((byte) 1, pdc);
    }

    @Test
    void trinketUniqueFalseRemovesKey() {
        ItemStack stack = sword();
        ItemMeta prepareMeta = Objects.requireNonNull(stack.getItemMeta());
        prepareMeta.getPersistentDataContainer()
                .set(KEY_TRINKET_UNIQUE, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(prepareMeta);

        ValhallaTrinketData trinkets = new ValhallaTrinketData(null, null, false);
        ValhallaItemData data = new ValhallaItemData(List.of(), List.of(), null, List.of(), trinkets);

        ItemStack result = ValhallaItemApplier.apply(stack, data);
        assertFalse(meta(result).getPersistentDataContainer()
                .has(KEY_TRINKET_UNIQUE, PersistentDataType.BYTE));
    }

    @Test
    void pdcKeysUseCorrectNamespaces() {
        assertEquals("valhallammo", KEY_ACTUAL_STATS.namespace());
        assertEquals("actual_stats", KEY_ACTUAL_STATS.value());
        assertEquals("valhallammo", KEY_DEFAULT_STATS.namespace());
        assertEquals("default_stats", KEY_DEFAULT_STATS.value());
        assertEquals("valhallammo", KEY_EQUIPMENT_CLASS.namespace());
        assertEquals("equipment_class", KEY_EQUIPMENT_CLASS.value());
        assertEquals("valhallammo", KEY_ITEM_FLAGS.namespace());
        assertEquals("item_flags", KEY_ITEM_FLAGS.value());
        assertEquals("valhallatrinkets", KEY_TRINKET_ID.namespace());
        assertEquals("trinket_id", KEY_TRINKET_ID.value());
        assertEquals("valhallatrinkets", KEY_TRINKET_UNIQUE_ID.namespace());
        assertEquals("trinket_unique_id", KEY_TRINKET_UNIQUE_ID.value());
        assertEquals("valhallatrinkets", KEY_TRINKET_UNIQUE.namespace());
        assertEquals("unique", KEY_TRINKET_UNIQUE.value());
    }

    @Test
    void emptyDataChangesNothing() {
        ValhallaItemData data = new ValhallaItemData(List.of(), List.of(), null, List.of(), null);
        ItemStack stack = sword();
        ItemStack result = ValhallaItemApplier.apply(stack, data);

        var pdc = meta(result).getPersistentDataContainer();
        assertFalse(pdc.has(KEY_ACTUAL_STATS, PersistentDataType.STRING));
        assertFalse(pdc.has(KEY_DEFAULT_STATS, PersistentDataType.STRING));
        assertFalse(pdc.has(KEY_EQUIPMENT_CLASS, PersistentDataType.STRING));
        assertFalse(pdc.has(KEY_ITEM_FLAGS, PersistentDataType.STRING));
    }
}
