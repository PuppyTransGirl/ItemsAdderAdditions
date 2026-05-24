package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class ItemDurabilityChangedTriggerHandler extends AbstractTriggerHandler {
    public ItemDurabilityChangedTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        String itemId = getItemId(event.getItem());
        if (itemId == null) return;
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.ITEM_DURABILITY_CHANGED)) {
            if (!(c.conditions() instanceof AdvancementConditions.ItemDurabilityChanged(String condItemId))) continue;
            if (condItemId != null && !condItemId.equals(itemId)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
