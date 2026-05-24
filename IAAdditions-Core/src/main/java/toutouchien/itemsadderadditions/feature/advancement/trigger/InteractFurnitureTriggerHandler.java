package toutouchien.itemsadderadditions.feature.advancement.trigger;

import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class InteractFurnitureTriggerHandler extends AbstractTriggerHandler {
    public InteractFurnitureTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(FurnitureInteractEvent event) {
        String id = event.getNamespacedID();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.INTERACT_FURNITURE)) {
            if (!(c.conditions() instanceof AdvancementConditions.InteractFurniture(String furnitureId))) continue;
            if (!furnitureId.equals(id)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
