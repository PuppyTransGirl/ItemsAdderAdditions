package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

/**
 * Awards the {@link RuntimeTrigger#JOIN_SERVER} criterion every time a player
 * joins the server.
 */
@NullMarked
public final class JoinServerTriggerHandler extends AbstractTriggerHandler {
    public JoinServerTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.JOIN_SERVER)) {
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
