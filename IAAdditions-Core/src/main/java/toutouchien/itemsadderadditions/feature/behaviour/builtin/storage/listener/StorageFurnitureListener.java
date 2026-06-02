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
        Entity entity = event.getBukkitEntity();
        if (entity == null) return;
        runtime.sessionManager().openForEntity(event.getPlayer(), entity);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        if (!event.getNamespacedID().equals(runtime.namespacedId())) return;

        Entity entity = event.getBukkitEntity();
        if (entity == null) return;

        // Flush any open session to the entity PDC, then read the live contents.
        runtime.sessionManager().closeSessionsAt(entity.getLocation(), null);

        // Survival can patch IA's normal dropped item. Creative usually has no IA drop, so the
        // storage runtime creates a content-bearing fallback for portable storage instead.
        Player breaker = event.getPlayer();
        boolean creative = breaker != null && breaker.getGameMode() == GameMode.CREATIVE;
        if (creative) {
            Log.debug("StorageFurnitureBreak", "Breaker in creative - using creative storage transfer.");
            ItemStack[] contents = StorageInventoryManager.loadFromEntity(entity, runtime.contentsKey());
            runtime.handleCreativeContainerBreak(entity.getLocation(), contents);
        } else {
            ItemStack[] contents = StorageInventoryManager.loadFromEntity(entity, runtime.contentsKey());
            runtime.handleContainerBreak(entity.getLocation(), contents);
        }
        StorageInventoryManager.clearEntity(entity, runtime.contentsKey());

        // IA's own barrier cleanup on furniture break is incomplete, so clear any orphaned
        // hitbox blocks ourselves a tick later.
        OpenVariantPlacement.removeFurnitureEntity(entity);
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
