package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class EnchantedItemTriggerHandler extends AbstractTriggerHandler {
    public EnchantedItemTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        int levels = event.getExpLevelCost();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.ENCHANTED_ITEM)) {
            if (!(c.conditions() instanceof AdvancementConditions.EnchantedItem(String itemId, int minLevels, int maxLevels)))
                continue;
            if (!matchesItem(event.getItem(), itemId)) continue;
            if (levels < minLevels || levels > maxLevels) continue;
            award(event.getEnchanter(), advancementKeyFor(c), c.name());
        }
    }
}
