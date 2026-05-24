package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class PlayerShearedEquipmentTriggerHandler extends AbstractTriggerHandler {
    public PlayerShearedEquipmentTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        String entityType = event.getEntity().getType().getKey().toString();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.PLAYER_SHEARED_EQUIPMENT)) {
            if (!(c.conditions() instanceof AdvancementConditions.PlayerShearedEquipment(String condEntityType))) continue;
            if (condEntityType != null && !condEntityType.equals(entityType)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
