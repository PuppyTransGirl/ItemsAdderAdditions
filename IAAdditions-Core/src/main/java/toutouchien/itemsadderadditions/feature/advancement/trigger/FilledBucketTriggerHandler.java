package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class FilledBucketTriggerHandler extends AbstractTriggerHandler {
    public FilledBucketTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFill(PlayerBucketFillEvent event) {
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.FILLED_BUCKET)) {
            if (!(c.conditions() instanceof AdvancementConditions.None)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
