package toutouchien.itemsadderadditions.feature.advancement.trigger;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class TickTriggerHandler extends AbstractTriggerHandler {
    public TickTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(ServerTickEndEvent event) {
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.TICK)) {
            for (Player player : Bukkit.getOnlinePlayers())
                award(player, advancementKeyFor(c), c.name());
        }
    }
}
