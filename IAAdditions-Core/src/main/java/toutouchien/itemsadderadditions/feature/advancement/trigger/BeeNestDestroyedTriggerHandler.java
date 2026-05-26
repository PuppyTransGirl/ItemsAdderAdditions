package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class BeeNestDestroyedTriggerHandler extends AbstractTriggerHandler {
    public BeeNestDestroyedTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Material mat = event.getBlock().getType();
        if (mat != Material.BEEHIVE && mat != Material.BEE_NEST) return;
        Player player = event.getPlayer();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.BEE_NEST_DESTROYED)) {
            if (!(c.conditions() instanceof AdvancementConditions.BeeNestDestroyed(String condBlockId))) continue;
            if (!matchesBlock(event.getBlock(), condBlockId)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
