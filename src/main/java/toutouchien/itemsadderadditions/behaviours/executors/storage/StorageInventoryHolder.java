package toutouchien.itemsadderadditions.behaviours.executors.storage;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NullMarked;

/**
 * {@link InventoryHolder} used as the owner of every storage GUI
 * created by {@link toutouchien.itemsadderadditions.behaviours.executors.StorageBehaviour}.
 *
 * <p>Carrying a {@link Location} as the holder identity lets event handlers determine
 * which storage block or furniture an inventory belongs to without needing to keep a
 * separate player-to-location map.
 */
@NullMarked
public final class StorageInventoryHolder implements InventoryHolder {
    private final Location location;
    private Inventory inventory;

    public StorageInventoryHolder(Location location) {
        this.location = location;
    }

    /**
     * Sets the inventory instance after it has been created.
     * Must be called before {@link #getInventory()} is used.
     */
    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Returns the world location of the block or furniture that owns this inventory.
     */
    public Location location() {
        return this.location;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
