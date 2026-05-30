package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class StorageInventoryManagerTest {
    private static final NamespacedKey CONTENTS_KEY = new NamespacedKey("itemsadderadditions", "contents");
    private static final NamespacedKey UNIQUE_KEY = new NamespacedKey("itemsadderadditions", "unique");

    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void injectIntoItemAndExtractFromItemRoundTripContents() {
        ItemStack item = ItemStack.of(Material.CHEST);
        ItemStack[] contents = {
                ItemStack.of(Material.DIAMOND, 3),
                null,
                ItemStack.of(Material.GOLD_INGOT, 5)
        };

        StorageInventoryManager.injectIntoItem(item, contents, CONTENTS_KEY);
        ItemStack[] extracted = StorageInventoryManager.extractFromItem(item, CONTENTS_KEY);

        assertNotNull(extracted);
        assertEquals(Material.DIAMOND, extracted[0].getType());
        assertEquals(3, extracted[0].getAmount());
        assertNull(extracted[1]);
        assertEquals(Material.GOLD_INGOT, extracted[2].getType());
        assertEquals(5, extracted[2].getAmount());
    }

    @Test
    void extractFromItemReturnsNullWhenKeyMissing() {
        ItemStack item = ItemStack.of(Material.CHEST);

        assertNull(StorageInventoryManager.extractFromItem(item, CONTENTS_KEY));
    }

    @Test
    void stampUniqueIdWritesDifferentUuidEachTime() {
        ItemStack first = ItemStack.of(Material.CHEST);
        ItemStack second = ItemStack.of(Material.CHEST);

        StorageInventoryManager.stampUniqueId(first, UNIQUE_KEY);
        StorageInventoryManager.stampUniqueId(second, UNIQUE_KEY);

        String firstId = first.getItemMeta().getPersistentDataContainer().get(UNIQUE_KEY, PersistentDataType.STRING);
        String secondId = second.getItemMeta().getPersistentDataContainer().get(UNIQUE_KEY, PersistentDataType.STRING);
        assertNotNull(firstId);
        assertNotNull(secondId);
        assertNotEquals(firstId, secondId);
    }

    @Test
    void populateInventoryIgnoresNullContents() {
        Inventory inventory = Bukkit.createInventory(null, 9);

        StorageInventoryManager.populateInventory(inventory, null);

        assertTrue(inventory.isEmpty());
    }

    @Test
    void populateInventoryCopiesUpToInventorySize() {
        Inventory inventory = Bukkit.createInventory(null, 9);
        ItemStack[] contents = new ItemStack[12];
        contents[0] = ItemStack.of(Material.STONE, 2);
        contents[8] = ItemStack.of(Material.DIRT, 4);
        contents[9] = ItemStack.of(Material.DIAMOND, 1);

        StorageInventoryManager.populateInventory(inventory, contents);

        assertEquals(Material.STONE, inventory.getItem(0).getType());
        assertEquals(2, inventory.getItem(0).getAmount());
        assertEquals(Material.DIRT, inventory.getItem(8).getType());
        assertEquals(4, inventory.getItem(8).getAmount());
        assertFalse(inventory.contains(Material.DIAMOND));
    }
}
