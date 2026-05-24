package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class KillEntityWithItemTriggerHandler extends AbstractTriggerHandler {
    public KillEntityWithItemTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        String iaId = getIaId(killer.getInventory().getItemInMainHand());
        if (iaId == null) return;
        String entityType = event.getEntity().getType().getKey().toString();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.KILL_ENTITY_WITH_ITEM)) {
            if (!(c.conditions() instanceof AdvancementConditions.KillEntityWithItem(String itemId, String type)))
                continue;
            if (!itemId.equals(iaId)) continue;
            if (type != null && !type.equals(entityType)) continue;
            award(killer, advancementKeyFor(c), c.name());
        }
    }
}
