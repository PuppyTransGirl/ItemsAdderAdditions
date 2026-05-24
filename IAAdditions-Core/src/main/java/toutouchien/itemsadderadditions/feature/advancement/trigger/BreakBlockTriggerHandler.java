package toutouchien.itemsadderadditions.feature.advancement.trigger;

import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class BreakBlockTriggerHandler extends AbstractTriggerHandler {
    public BreakBlockTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(CustomBlockBreakEvent event) {
        String blockId = event.getNamespacedID();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.BREAK_BLOCK)) {
            if (!(c.conditions() instanceof AdvancementConditions.BreakBlock(String id))) continue;
            if (!id.equals(blockId)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
