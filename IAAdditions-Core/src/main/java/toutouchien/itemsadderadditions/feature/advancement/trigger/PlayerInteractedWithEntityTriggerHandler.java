package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class PlayerInteractedWithEntityTriggerHandler extends AbstractTriggerHandler {
    public PlayerInteractedWithEntityTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        String entityType = event.getRightClicked().getType().getKey().toString();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.PLAYER_INTERACTED_WITH_ENTITY)) {
            if (!(c.conditions() instanceof AdvancementConditions.PlayerInteractedWithEntity(String condEntityType, String condItemId))) continue;
            if (condEntityType != null && !condEntityType.equals(entityType)) continue;
            if (!matchesItem(player.getInventory().getItemInMainHand(), condItemId)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
