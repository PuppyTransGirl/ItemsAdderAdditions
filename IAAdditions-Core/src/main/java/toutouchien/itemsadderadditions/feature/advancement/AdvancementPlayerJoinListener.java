package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.NmsManager;

@NullMarked
public final class AdvancementPlayerJoinListener implements Listener {
    private final AdvancementRegistry registry;
    private final Plugin plugin;

    public AdvancementPlayerJoinListener(AdvancementRegistry registry, Plugin plugin) {
        this.registry = registry;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        NmsManager.instance().handler().advancements()
                .onPlayerJoin(player, registry.rootKeys());
        // Delayed 1 tick: Minecraft sends the initial advancement sync on the first tick after
        // join, so we remove hidden not-yet-completed ones after that sync has been sent.
        plugin.getServer().getScheduler().runTask(plugin, () ->
                NmsManager.instance().handler().advancements()
                        .removeIncompleteHiddenAdvancements(player, registry.hiddenKeys()));
    }
}
