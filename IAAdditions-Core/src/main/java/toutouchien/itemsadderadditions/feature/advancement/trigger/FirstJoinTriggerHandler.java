package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

/**
 * Awards the {@link RuntimeTrigger#FIRST_JOIN} criterion the first time a player
 * ever joins the server.
 */
@NullMarked
public final class FirstJoinTriggerHandler extends AbstractTriggerHandler {
    public FirstJoinTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) return;
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.FIRST_JOIN)) {
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
