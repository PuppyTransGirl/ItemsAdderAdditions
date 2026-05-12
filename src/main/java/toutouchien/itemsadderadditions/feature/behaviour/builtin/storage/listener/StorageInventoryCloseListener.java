package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageInventoryHolder;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageRuntime;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageSession;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageType;

@NullMarked
public final class StorageInventoryCloseListener implements Listener {
    private final StorageRuntime runtime;

    public StorageInventoryCloseListener(StorageRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder(false) instanceof StorageInventoryHolder holder)) return;

        StorageSession session = runtime.sessionManager().remove(player.getUniqueId());
        if (session == null) return;

        if (session.type() == StorageType.DISPOSAL) {
            runtime.sessionManager().executeClose(holder.location(), true);
            return;
        }

        runtime.sessionManager().saveSessionContents(session, true);
    }
}
