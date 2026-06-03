package toutouchien.itemsadderadditions.feature.advancement.trigger;

import dev.lone.itemsadder.api.Events.ComplexFurnitureSitEvent;
import dev.lone.itemsadder.api.Events.FurnitureSitEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

/**
 * Awards the {@link RuntimeTrigger#SIT_FURNITURE} criterion when a player sits on
 * a custom furniture, including complex furniture.
 *
 * <p>The sit events only exist in ItemsAdder 4.0.17 and up, so this handler must
 * never be loaded on older builds. {@code AdvancementRuntimeService} gates its
 * instantiation behind a version check.</p>
 */
@NullMarked
public final class SitFurnitureTriggerHandler extends AbstractTriggerHandler {
    public SitFurnitureTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSit(FurnitureSitEvent event) {
        handle(event.getPlayer(), event.getNamespacedID());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onComplexSit(ComplexFurnitureSitEvent event) {
        handle(event.getPlayer(), event.getNamespacedID());
    }

    private void handle(Player player, String furnitureId) {
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.SIT_FURNITURE)) {
            if (!(c.conditions() instanceof AdvancementConditions.SitFurniture(String expected))) continue;
            if (!matchesFurniture(furnitureId, expected)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
