package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class ShootBowTriggerHandler extends AbstractTriggerHandler {
    public ShootBowTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        String iaId = getIaId(event.getBow());
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.SHOOT_BOW)) {
            if (!(c.conditions() instanceof AdvancementConditions.ShootBow(String itemId))) continue;
            if (itemId != null && !itemId.equals(iaId)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
