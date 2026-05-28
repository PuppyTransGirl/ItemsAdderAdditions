package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.listener;

import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
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
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantPlacement;

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
        Log.debug("StorageOpenVariantBreak", "FurnitureBreakEvent fired: id={}, runtimeId={}, openVariantId={}, entity={}",
                event.getNamespacedID(), runtime.namespacedId(),
                runtime.openVariantConfig() == null ? "null" : runtime.openVariantConfig().id(),
                event.getBukkitEntity());

        if (!runtime.hasFurnitureOpenVariant()) {
            Log.debug("StorageOpenVariantBreak", "Ignoring: no furniture open variant configured.");
            return;
        }
        if (!runtime.matchesOpenVariantId(event.getNamespacedID())) {
            Log.debug("StorageOpenVariantBreak", "Ignoring: id does not match open variant.");
            return;
        }

        Entity entity = event.getBukkitEntity();
        if (!runtime.openVariantTransformer().isTransformed(entity.getLocation())) {
            Log.debug("StorageOpenVariantBreak", "Ignoring: location {} is not in transformed state.",
                    entity.getLocation());
            return;
        }

        ItemStack[] contents = runtime.sessionManager().getLiveContentsAt(entity.getLocation());
        Log.debug("StorageOpenVariantBreak", "Loaded live contents: hasContents={}", contents != null);

        runtime.sessionManager().closeSessionsForOpenVariantBreak(entity.getLocation());
        runtime.openVariantTransformer().forgetState(entity.getLocation());

        Player breaker = event.getPlayer();
        boolean creative = breaker != null && breaker.getGameMode() == GameMode.CREATIVE;
        if (creative) {
            Log.debug("StorageOpenVariantBreak", "Breaker in creative - skipping drops.");
        } else {
            runtime.handleOpenVariantBreakDrops(entity.getLocation(), contents);
        }

        Log.debug("StorageOpenVariantBreak", "Calling removeFurnitureEntity on {} (valid={}, type={})",
                entity, entity.isValid(), entity.getType());
        OpenVariantPlacement.removeFurnitureEntity(entity);
        Log.debug("StorageOpenVariantBreak", "Removal completed. Entity now valid={}, dead={}",
                entity.isValid(), entity.isDead());

        OpenVariantPlacement.scheduleBarrierSweep(runtime.plugin(), entity.getLocation(), 1);
    }
}
