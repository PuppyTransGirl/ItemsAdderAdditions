package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.utils.Task;

import java.util.concurrent.TimeUnit;

@NullMarked
public final class TextDisplayPlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final TextDisplayRuntime runtime;

    public TextDisplayPlayerListener(JavaPlugin plugin, TextDisplayRuntime runtime) {
        this.plugin = plugin;
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Task.syncLater(task -> runtime.resync(event.getPlayer()), plugin, 50, TimeUnit.MILLISECONDS);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        runtime.forgetViewer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Task.syncLater(task -> runtime.resync(event.getPlayer()), plugin, 50, TimeUnit.MILLISECONDS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Task.syncLater(task -> runtime.resync(event.getPlayer()), plugin, 50, TimeUnit.MILLISECONDS);
    }
}
