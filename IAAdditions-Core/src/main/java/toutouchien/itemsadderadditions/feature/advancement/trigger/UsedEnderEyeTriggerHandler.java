package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class UsedEnderEyeTriggerHandler extends AbstractTriggerHandler {
    public UsedEnderEyeTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        var item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_EYE) return;
        Player player = event.getPlayer();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.USED_ENDER_EYE)) {
            if (!(c.conditions() instanceof AdvancementConditions.None)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
