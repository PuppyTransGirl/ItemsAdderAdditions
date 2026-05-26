package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class ShotCrossbowTriggerHandler extends AbstractTriggerHandler {
    public ShotCrossbowTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getBow() == null || event.getBow().getType() != Material.CROSSBOW) return;
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.SHOT_CROSSBOW)) {
            if (!(c.conditions() instanceof AdvancementConditions.ShotCrossbow(String condItemId))) continue;
            if (!matchesItem(event.getBow(), condItemId)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
