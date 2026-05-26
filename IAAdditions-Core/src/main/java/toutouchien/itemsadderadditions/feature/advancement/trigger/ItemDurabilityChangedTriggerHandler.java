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
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.ITEM_DURABILITY_CHANGED)) {
            if (!(c.conditions() instanceof AdvancementConditions.ItemDurabilityChanged(String condItemId))) continue;
            if (!matchesItem(event.getItem(), condItemId)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
