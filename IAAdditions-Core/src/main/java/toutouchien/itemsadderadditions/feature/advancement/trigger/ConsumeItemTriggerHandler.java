package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class ConsumeItemTriggerHandler extends AbstractTriggerHandler {
    public ConsumeItemTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        String iaId = getIaId(event.getItem());
        if (iaId == null) return;
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.CONSUME_ITEM)) {
            if (!(c.conditions() instanceof AdvancementConditions.ConsumeItem(String itemId))) continue;
            if (!itemId.equals(iaId)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
