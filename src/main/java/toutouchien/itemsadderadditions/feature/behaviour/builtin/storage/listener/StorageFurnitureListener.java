package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.listener;

import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import dev.lone.itemsadder.api.Events.FurniturePlacedEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageInventoryManager;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageRuntime;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageType;

@NullMarked
public final class StorageFurnitureListener implements Listener {
    private final StorageRuntime runtime;

    public StorageFurnitureListener(StorageRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        if (!event.getNamespacedID().equals(runtime.namespacedId())) return;
        if (event.getPlayer().isSneaking()) return;

        event.setCancelled(true);
        runtime.sessionManager().openForEntity(event.getPlayer(), event.getBukkitEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        if (!event.getNamespacedID().equals(runtime.namespacedId())) return;

        Entity entity = event.getBukkitEntity();
        runtime.sessionManager().closeSessionsAt(entity.getLocation(), null);

        ItemStack[] contents = StorageInventoryManager.loadFromEntity(
                entity,
                runtime.contentsKey()
        );
        runtime.handleContainerBreak(entity.getLocation(), contents);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlaced(FurniturePlacedEvent event) {
        if (!event.getNamespacedID().equals(runtime.namespacedId())) return;
        if (runtime.storageType() != StorageType.SHULKER) return;

        ItemStack[] stored = runtime.shulkerDropTracker()
                .consumePlaceContents(event.getPlayer().getUniqueId());
        if (stored == null) return;

        StorageInventoryManager.saveToEntity(
                event.getBukkitEntity(),
                stored,
                runtime.contentsKey()
        );
    }
}
