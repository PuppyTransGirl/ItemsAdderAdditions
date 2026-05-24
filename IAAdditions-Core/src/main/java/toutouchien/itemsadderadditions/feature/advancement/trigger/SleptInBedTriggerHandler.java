package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class SleptInBedTriggerHandler extends AbstractTriggerHandler {
    public SleptInBedTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.enterAction().problem() != null) return;
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.SLEPT_IN_BED)) {
            if (!(c.conditions() instanceof AdvancementConditions.None)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
