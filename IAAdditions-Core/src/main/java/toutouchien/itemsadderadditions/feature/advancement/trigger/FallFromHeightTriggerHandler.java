package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class FallFromHeightTriggerHandler extends AbstractTriggerHandler {
    public FallFromHeightTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        double fallDistance = player.getFallDistance();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.FALL_FROM_HEIGHT)) {
            if (!(c.conditions() instanceof AdvancementConditions.FallFromHeight(double minDistance, double maxDistance))) {
                continue;
            }
            if (fallDistance < minDistance || fallDistance > maxDistance) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
