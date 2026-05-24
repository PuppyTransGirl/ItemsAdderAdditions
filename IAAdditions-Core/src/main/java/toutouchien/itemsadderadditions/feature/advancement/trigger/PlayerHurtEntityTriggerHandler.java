package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class PlayerHurtEntityTriggerHandler extends AbstractTriggerHandler {
    public PlayerHurtEntityTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        String entityType = event.getEntity().getType().getKey().toString();
        String iaId = getIaId(player.getInventory().getItemInMainHand());
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.PLAYER_HURT_ENTITY)) {
            if (!(c.conditions() instanceof AdvancementConditions.PlayerHurtEntity(String itemId, String type)))
                continue;
            if (itemId != null && !itemId.equals(iaId)) continue;
            if (type != null && !type.equals(entityType)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
