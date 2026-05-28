package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.listener;

import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import dev.lone.itemsadder.api.Events.FurniturePlacedEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageRuntime;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageType;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryManager;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantPlacement;

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
        Log.debug("StorageFurnitureBreak", "FurnitureBreakEvent fired: id={}, runtimeId={}, entity={}, loc={}",
                event.getNamespacedID(), runtime.namespacedId(), event.getBukkitEntity(),
                event.getBukkitEntity() == null ? "null" : event.getBukkitEntity().getLocation());

        if (!event.getNamespacedID().equals(runtime.namespacedId())) {
            Log.debug("StorageFurnitureBreak", "Ignoring break: id mismatch.");
            return;
        }

        Entity entity = event.getBukkitEntity();
        Log.debug("StorageFurnitureBreak", "Closing sessions at {} (storageType={})",
                entity.getLocation(), runtime.storageType());
        runtime.sessionManager().closeSessionsAt(entity.getLocation(), null);

        Player breaker = event.getPlayer();
        boolean creative = breaker != null && breaker.getGameMode() == GameMode.CREATIVE;
        if (creative) {
            Log.debug("StorageFurnitureBreak", "Breaker in creative - skipping drops.");
        } else {
            ItemStack[] contents = StorageInventoryManager.loadFromEntity(
                    entity,
                    runtime.contentsKey()
            );
            Log.debug("StorageFurnitureBreak", "Loaded contents from entity: hasContents={}",
                    contents != null);

            runtime.handleContainerBreak(entity.getLocation(), contents);
        }

        Log.debug("StorageFurnitureBreak", "Calling removeFurnitureEntity on {} (valid={}, type={})",
                entity, entity.isValid(), entity.getType());
        OpenVariantPlacement.removeFurnitureEntity(entity);
        Log.debug("StorageFurnitureBreak", "Removal completed. Entity now valid={}, dead={}",
                entity.isValid(), entity.isDead());

        OpenVariantPlacement.scheduleBarrierSweep(runtime.plugin(), entity.getLocation(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlaced(FurniturePlacedEvent event) {
        if (!event.getNamespacedID().equals(runtime.namespacedId())) return;
        if (runtime.storageType() != StorageType.SHULKER) return;

        // Yes, it can be null
        if (event.getPlayer() == null)
            return;

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
