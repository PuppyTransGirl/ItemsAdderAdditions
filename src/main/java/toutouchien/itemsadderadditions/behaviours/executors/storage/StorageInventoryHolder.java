package toutouchien.itemsadderadditions.behaviours.executors.storage;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class StorageInventoryHolder implements InventoryHolder {
    private Inventory inventory;
    private final Location location;

    public StorageInventoryHolder(Location location) {
        this.location = location;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Location location() {
        return this.location;
    }

    /**
     * Get the object's inventory.
     *
     * @return The inventory.
     */
    @NotNull
    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
