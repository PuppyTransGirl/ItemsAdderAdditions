package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class KilledByArrowTriggerHandler extends AbstractTriggerHandler {
    public KilledByArrowTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = (Player) event.getEntity();
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (!(lastDamage instanceof EntityDamageByEntityEvent damageByEntity)) return;
        if (!(damageByEntity.getDamager() instanceof AbstractArrow arrow)) return;
        Entity shooter = arrow.getShooter() instanceof Entity e ? e : null;
        String entityType = shooter != null ? shooter.getType().getKey().toString()
                : arrow.getType().getKey().toString();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.KILLED_BY_ARROW)) {
            if (!(c.conditions() instanceof AdvancementConditions.KilledByArrow(String condEntityType))) continue;
            if (condEntityType != null && !condEntityType.equals(entityType)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
