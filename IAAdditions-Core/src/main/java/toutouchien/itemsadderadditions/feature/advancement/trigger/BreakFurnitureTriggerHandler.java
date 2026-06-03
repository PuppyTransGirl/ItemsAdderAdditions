package toutouchien.itemsadderadditions.feature.advancement.trigger;

import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class BreakFurnitureTriggerHandler extends AbstractTriggerHandler {
    public BreakFurnitureTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(FurnitureBreakEvent event) {
        String id = event.getNamespacedID();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.BREAK_FURNITURE)) {
            if (!(c.conditions() instanceof AdvancementConditions.BreakFurniture(String furnitureId))) continue;
            if (!matchesFurniture(id, furnitureId)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
