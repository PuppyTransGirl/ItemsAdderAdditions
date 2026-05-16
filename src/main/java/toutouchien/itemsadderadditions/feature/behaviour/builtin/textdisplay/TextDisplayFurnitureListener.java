package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurniturePlacedEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

/**
 * Tracks furniture placements and breaks for a {@link TextDisplayRuntime}.
 */
@NullMarked
public final class TextDisplayFurnitureListener implements Listener {
    private final TextDisplayRuntime runtime;

    public TextDisplayFurnitureListener(TextDisplayRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlaced(FurniturePlacedEvent event) {
        if (!runtime.matchesId(event.getNamespacedID())) return;

        Entity entity = event.getBukkitEntity();
        runtime.track(TextDisplayOwner.furniture(runtime.namespacedId(), runtime.category(), entity));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        if (!runtime.matchesId(event.getNamespacedID())) return;

        runtime.untrack(event.getBukkitEntity().getUniqueId());
    }
}
