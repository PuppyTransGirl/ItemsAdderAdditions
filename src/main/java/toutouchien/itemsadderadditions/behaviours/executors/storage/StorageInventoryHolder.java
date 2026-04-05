package toutouchien.itemsadderadditions.behaviours.executors.storage;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class StorageInventoryHolder implements InventoryHolder {
    @Override
    public Inventory getInventory() {
        throw new UnsupportedOperationException("Plugin trying to access Inventory of StorageInventoryHolder");
    }
}
