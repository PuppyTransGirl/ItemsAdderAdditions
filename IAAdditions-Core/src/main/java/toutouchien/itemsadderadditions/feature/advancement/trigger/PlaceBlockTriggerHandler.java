package toutouchien.itemsadderadditions.feature.advancement.trigger;

import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class PlaceBlockTriggerHandler extends AbstractTriggerHandler {
    public PlaceBlockTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(CustomBlockPlaceEvent event) {
        String blockId = event.getNamespacedID();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.PLACE_BLOCK)) {
            if (!(c.conditions() instanceof AdvancementConditions.PlaceBlock(String id))) continue;
            if (!id.equals(blockId)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
