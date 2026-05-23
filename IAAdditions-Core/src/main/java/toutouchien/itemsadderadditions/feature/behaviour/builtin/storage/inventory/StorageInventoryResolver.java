package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageType;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.session.StorageSessionRegistry;

@NullMarked
@SuppressWarnings("UnstableApiUsage")
public final class StorageInventoryResolver {
    private final StorageSessionRegistry sessions;
    private final int rows;
    @Nullable private final StorageInventorySpec spec;
    private final Component title;
    private final StorageType storageType;
    private final NamespacedKey contentsKey;
    private final JavaPlugin plugin;

    public StorageInventoryResolver(
            StorageSessionRegistry sessions,
            int rows,
            @Nullable StorageInventorySpec spec,
            Component title,
            StorageType storageType,
            NamespacedKey contentsKey,
            JavaPlugin plugin
    ) {
        this.sessions = sessions;
        this.rows = rows;
        this.spec = spec;
        this.title = title;
        this.storageType = storageType;
        this.contentsKey = contentsKey;
        this.plugin = plugin;
    }

    /**
     * Creates (or retrieves) the inventory for {@code location}, populates it with saved
     * contents, opens it for {@code player}, and returns it.
     *
     * <p>For {@link StorageInventorySpec.Menu} types the inventory is opened via
     * {@link InventoryView#open()} so the client receives a fully functional menu
     * (smelting, brewing, etc.). For all other types {@link Player#openInventory(Inventory)}
     * is used.</p>
     */
    public Inventory openFor(Player player, Location location, @Nullable Block block, @Nullable Entity entity) {
        if (storageType != StorageType.DISPOSAL) {
            Inventory live = sessions.liveInventoryAt(location);
            if (live != null) {
                player.openInventory(live);
                return live;
            }
        }

        ItemStack[] contents = storedContents(block, entity);

        if (spec instanceof StorageInventorySpec.Menu menu) {
            InventoryView view = menu.menuType().create(player, title);
            Inventory inventory = view.getTopInventory();
            StorageInventoryManager.populateInventory(inventory, contents);
            view.open();
            return inventory;
        }

        StorageInventoryHolder holder = new StorageInventoryHolder(location);
        Inventory inventory = spec instanceof StorageInventorySpec.Typed typed
                ? Bukkit.createInventory(holder, typed.inventoryType(), title)
                : Bukkit.createInventory(holder, rows * 9, title);
        holder.inventory(inventory);
        StorageInventoryManager.populateInventory(inventory, contents);
        player.openInventory(inventory);
        return inventory;
    }

    @Nullable
    public ItemStack[] liveContentsAt(Location location) {
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
