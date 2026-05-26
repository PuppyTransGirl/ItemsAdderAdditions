package toutouchien.itemsadderadditions.feature.advancement.trigger;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class VillagerTradeTriggerHandler extends AbstractTriggerHandler {
    public VillagerTradeTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(PlayerTradeEvent event) {
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.VILLAGER_TRADE)) {
            if (!(c.conditions() instanceof AdvancementConditions.VillagerTrade(String itemId))) continue;
            if (!matchesItem(event.getTrade().getResult(), itemId)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
