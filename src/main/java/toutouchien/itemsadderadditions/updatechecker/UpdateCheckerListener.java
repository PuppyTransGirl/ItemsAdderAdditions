package toutouchien.itemsadderadditions.updatechecker;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public class UpdateCheckerListener implements Listener {
    private final JavaPlugin plugin;
    private final String currentVersion;
    private final String latestVersion;

    public UpdateCheckerListener(JavaPlugin plugin, String currentVersion, String latestVersion) {
        this.plugin = plugin;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!this.plugin.getConfig().getBoolean("update-checker.on-join", true))
            return;

        Player player = event.getPlayer();
        String pluginName = this.plugin.getName().toLowerCase(Locale.ROOT);
        if (!player.hasPermission(pluginName + ".update-checker"))
            return;

        player.sendMessage("PLACEHOLDER".formatted(this.currentVersion, this.latestVersion));
    }
}
