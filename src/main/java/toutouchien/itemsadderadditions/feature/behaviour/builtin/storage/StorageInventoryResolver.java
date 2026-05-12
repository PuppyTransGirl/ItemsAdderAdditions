package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class StorageInventoryResolver {
    private final StorageSessionRegistry sessions;
    private final int rows;
    private final Component title;
    private final StorageType storageType;
    private final NamespacedKey contentsKey;
    private final JavaPlugin plugin;

    StorageInventoryResolver(
            StorageSessionRegistry sessions,
            int rows,
            Component title,
            StorageType storageType,
            NamespacedKey contentsKey,
            JavaPlugin plugin
    ) {
        this.sessions = sessions;
        this.rows = rows;
        this.title = title;
        this.storageType = storageType;
        this.contentsKey = contentsKey;
        this.plugin = plugin;
    }

    Inventory resolve(Location location, @Nullable Block block, @Nullable Entity entity) {
        if (storageType != StorageType.DISPOSAL) {
            Inventory live = sessions.liveInventoryAt(location);
            if (live != null) {
                return live;
            }
        }

        StorageInventoryHolder holder = new StorageInventoryHolder(location);
        Inventory inventory = Bukkit.createInventory(holder, rows * 9, title);
        holder.inventory(inventory);
        StorageInventoryManager.populateInventory(inventory, storedContents(block, entity));
        return inventory;
    }

    @Nullable
    ItemStack[] liveContentsAt(Location location) {
        Inventory live = sessions.liveInventoryAt(location);
        return live == null ? null : live.getContents();
    }

    @Nullable
    private ItemStack[] storedContents(@Nullable Block block, @Nullable Entity entity) {
        return switch (storageType) {
            case STORAGE, SHULKER -> block != null
                    ? StorageInventoryManager.loadFromBlock(block, contentsKey, plugin)
                    : StorageInventoryManager.loadFromEntity(entity, contentsKey);
            case DISPOSAL -> null;
        };
    }
}
