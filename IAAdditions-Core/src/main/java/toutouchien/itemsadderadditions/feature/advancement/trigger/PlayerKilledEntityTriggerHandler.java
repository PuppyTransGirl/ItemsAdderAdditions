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
public final class PlayerKilledEntityTriggerHandler extends AbstractTriggerHandler {
    public PlayerKilledEntityTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        String entityType = event.getEntity().getType().getKey().toString();
        String heldItemId = getItemId(killer.getInventory().getItemInMainHand());
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.PLAYER_KILLED_ENTITY)) {
            if (!(c.conditions() instanceof AdvancementConditions.PlayerKilledEntity(String condEntityType, String condItemId)))
                continue;
            if (condEntityType != null && !condEntityType.equals(entityType)) continue;
            if (condItemId != null && !condItemId.equals(heldItemId)) continue;
            award(killer, advancementKeyFor(c), c.name());
        }
    }
}
