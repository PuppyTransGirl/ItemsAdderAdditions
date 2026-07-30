package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.utils.BiomeKeys;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class InBiomeTriggerHandler extends AbstractTriggerHandler {
    public InBiomeTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        NamespacedKey fromBiome = BiomeKeys.key(event.getFrom().getBlock().getBiome());
        NamespacedKey toBiome = BiomeKeys.key(event.getTo().getBlock().getBiome());
        if (fromBiome.equals(toBiome)) return;

        String worldName = event.getPlayer().getWorld().getName();
        String biomeStr = toBiome.getNamespace() + ":" + toBiome.getKey();

        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.IN_BIOME)) {
            if (!(c.conditions() instanceof AdvancementConditions.InBiome(String biomeId, String world))) continue;
            if (!biomeId.equals(biomeStr)) continue;
            if (world != null && !world.equals(worldName)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
