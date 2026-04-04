package toutouchien.itemsadderadditions.behaviours.executors.storage;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@NullMarked
public final class StorageInventoryManager {
    private static final AtomicBoolean CBD_REGISTERED = new AtomicBoolean(false);

    private StorageInventoryManager() {
        throw new IllegalStateException("Static class");
    }

    public static void ensureCustomBlockDataRegistered(JavaPlugin plugin) {
        if (CBD_REGISTERED.compareAndSet(false, true)) {
            CustomBlockData.registerListener(plugin);
            Log.debug("StorageManager", "CustomBlockData Listener Registered.");
        }
    }

    public static void saveToBlock(Block block, ItemStack[] contents, NamespacedKey key, JavaPlugin plugin) {
        new CustomBlockData(block, plugin).set(key, DataType.ITEM_STACK_ARRAY, contents);
        Log.debug("StorageManager", "Saved " + contents.length + " slot array to CustomBlockData for block at " + block.getLocation());
    }

    @Nullable
    public static ItemStack[] loadFromBlock(Block block, NamespacedKey key, JavaPlugin plugin) {
        ItemStack[] contents = new CustomBlockData(block, plugin).get(key, DataType.ITEM_STACK_ARRAY);
        Log.debug("StorageManager", "Loaded contents from CustomBlockData. Is null? " + (contents == null));
        return contents;
    }

    public static void clearBlock(Block block, JavaPlugin plugin) {
        new CustomBlockData(block, plugin).clear();
    }

    public static void saveToEntity(Entity entity, ItemStack[] contents, NamespacedKey key) {
        entity.getPersistentDataContainer().set(key, DataType.ITEM_STACK_ARRAY, contents);
        Log.debug("StorageManager", "Saved " + contents.length + " slot array to Entity PDC (UUID: " + entity.getUniqueId() + ")");
    }

    @Nullable
    public static ItemStack[] loadFromEntity(Entity entity, NamespacedKey key) {
        ItemStack[] contents = entity.getPersistentDataContainer().get(key, DataType.ITEM_STACK_ARRAY);
        Log.debug("StorageManager", "Loaded contents from Entity PDC. Is null? " + (contents == null));
        return contents;
    }

    public static void injectIntoItem(ItemStack item, ItemStack[] contents, NamespacedKey key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            Log.warn("StorageManager", "Failed to inject contents into item: ItemMeta is null!");
            return;
        }

        meta.getPersistentDataContainer().set(key, DataType.ITEM_STACK_ARRAY, contents);
        item.setItemMeta(meta);
        Log.debug("StorageManager", "Successfully injected inventory into ItemStack Meta.");
    }

    /**
     * Stamps a fresh random UUID onto the item's PDC under {@code key}.
     * This makes the item unique so it cannot stack with other instances.
     */
    public static void stampUniqueId(ItemStack item, NamespacedKey key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, UUID.randomUUID().toString());
        item.setItemMeta(meta);
        Log.debug("StorageManager", "Stamped unique ID onto ItemStack.");
    }

    @Nullable
    public static ItemStack[] extractFromItem(ItemStack item, NamespacedKey key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return null;

        return meta.getPersistentDataContainer().get(key, DataType.ITEM_STACK_ARRAY);
    }

    public static void populateInventory(Inventory inventory, @Nullable ItemStack[] contents) {
        if (contents == null)
            return;

        int slots = Math.min(contents.length, inventory.getSize());
        for (int i = 0; i < slots; i++)
            inventory.setItem(i, contents[i]);
    }
}
