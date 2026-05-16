package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

/**
 * Tracks custom block placements and breaks for a {@link TextDisplayRuntime}.
 */
@NullMarked
public final class TextDisplayBlockListener implements Listener {
    private final TextDisplayRuntime runtime;

    public TextDisplayBlockListener(TextDisplayRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(CustomBlockPlaceEvent event) {
        if (!runtime.matchesId(event.getNamespacedID())) return;

        float yaw = TextDisplayLocationMath.blockYaw(event.getNamespacedID(), event.getPlayer().getLocation().getYaw());
        runtime.track(TextDisplayOwner.block(runtime.namespacedId(), event.getBlock(), yaw));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(CustomBlockBreakEvent event) {
        if (!runtime.matchesId(event.getNamespacedID())) return;

        Block block = event.getBlock();
        runtime.untrack(TextDisplayOwner.blockOwnerId(block.getLocation()));
    }
}
