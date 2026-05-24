package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class FishingRodHookedTriggerHandler extends AbstractTriggerHandler {
    public FishingRodHookedTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                && event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;
        String rodItemId = getItemId(event.getPlayer().getInventory().getItemInMainHand());
        String caughtEntityType = event.getCaught() != null
                ? event.getCaught().getType().getKey().toString() : null;
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.FISHING_ROD_HOOKED)) {
            if (!(c.conditions() instanceof AdvancementConditions.FishingRodHooked(String rod, String caughtType)))
                continue;
            if (rod != null && !rod.equals(rodItemId)) continue;
            if (caughtType != null && !caughtType.equals(caughtEntityType)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
