package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.listener;

import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageRuntime;

@NullMarked
public final class StorageOpenVariantFurnitureListener implements Listener {
    private final StorageRuntime runtime;

    public StorageOpenVariantFurnitureListener(StorageRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Player right-clicks the open-form furniture. Open the GUI without transforming again.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpenVariantFurnitureInteract(FurnitureInteractEvent event) {
        if (!runtime.hasFurnitureOpenVariant()) return;
        if (!runtime.matchesOpenVariantId(event.getNamespacedID())) return;
        if (event.getPlayer().isSneaking()) return;

        Entity entity = event.getBukkitEntity();
        if (!runtime.openVariantTransformer().isTransformed(entity.getLocation())) return;

        event.setCancelled(true);
        runtime.sessionManager().openForPlayerAtTransformedLocation(
                event.getPlayer(),
                entity.getLocation(),
                null,
                entity
        );
    }

    /**
     * The open-form furniture was broken: restore tracking and drop the original item.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpenVariantFurnitureBreak(FurnitureBreakEvent event) {
        if (!runtime.hasFurnitureOpenVariant()) return;
        if (!runtime.matchesOpenVariantId(event.getNamespacedID())) return;

        Entity entity = event.getBukkitEntity();
        if (!runtime.openVariantTransformer().isTransformed(entity.getLocation())) return;

        ItemStack[] contents = runtime.sessionManager().getLiveContentsAt(entity.getLocation());

        runtime.sessionManager().closeSessionsAt(entity.getLocation(), null);
        runtime.openVariantTransformer().forceRemove(entity.getLocation());

        runtime.handleOpenVariantBreakDrops(entity.getLocation(), contents);
    }
}
