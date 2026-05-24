package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.NmsManager;

@NullMarked
public final class AdvancementPlayerJoinListener implements Listener {
    private final AdvancementRegistry registry;

    public AdvancementPlayerJoinListener(AdvancementRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        NmsManager.instance().handler().advancements()
                .onPlayerJoin(event.getPlayer(), registry.rootKeys());
    }
}
