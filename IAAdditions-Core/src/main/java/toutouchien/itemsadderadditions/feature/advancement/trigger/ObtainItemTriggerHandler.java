package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class ObtainItemTriggerHandler extends AbstractTriggerHandler {
    public ObtainItemTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        String itemId = getItemId(event.getItem().getItemStack());
        if (itemId == null) return;
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.OBTAIN_ITEM)) {
            if (!(c.conditions() instanceof AdvancementConditions.ObtainItem(
                    java.util.List<String> itemIds, int amount
            ))) continue;
            if (!itemIds.contains(itemId)) continue;
            if (event.getItem().getItemStack().getAmount() < amount) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
