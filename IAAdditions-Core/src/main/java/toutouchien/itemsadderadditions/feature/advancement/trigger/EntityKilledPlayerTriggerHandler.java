package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
public final class EntityKilledPlayerTriggerHandler extends AbstractTriggerHandler {
    public EntityKilledPlayerTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (!(lastDamage instanceof EntityDamageByEntityEvent damageByEntity)) return;
        Entity damager = damageByEntity.getDamager();
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }
        if (damager instanceof Player) return;
        String entityType = damager.getType().getKey().toString();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.ENTITY_KILLED_PLAYER)) {
            if (!(c.conditions() instanceof AdvancementConditions.EntityKilledPlayer(String condEntityType))) continue;
            if (condEntityType != null && !condEntityType.equals(entityType)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
