package toutouchien.itemsadderadditions.feature.advancement.trigger;

import dev.lone.itemsadder.api.Events.FurniturePlacedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

@NullMarked
public final class PlaceFurnitureTriggerHandler extends AbstractTriggerHandler {
    public PlaceFurnitureTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(FurniturePlacedEvent event) {
        String id = event.getNamespacedID();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.PLACE_FURNITURE)) {
            if (!(c.conditions() instanceof AdvancementConditions.PlaceFurniture(String furnitureId))) continue;
            if (!NamespaceUtils.matchesWithRotation(id, furnitureId)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
