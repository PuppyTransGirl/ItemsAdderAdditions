package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class ChangedDimensionTriggerHandler extends AbstractTriggerHandler {
    public ChangedDimensionTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    private static String dimensionKey(World.Environment env) {
        return switch (env) {
            case NETHER -> "nether";
            case THE_END -> "the_end";
            default -> "normal";
        };
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        String dimension = dimensionKey(event.getPlayer().getWorld().getEnvironment());
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.CHANGED_DIMENSION)) {
            if (!(c.conditions() instanceof AdvancementConditions.ChangedDimension(String dimension1))) continue;
            if (dimension1 != null && !dimension1.equals(dimension)) continue;
            award(event.getPlayer(), advancementKeyFor(c), c.name());
        }
    }
}
