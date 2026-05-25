package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

import java.util.List;

@NullMarked
public final class ObtainItemTriggerHandler extends AbstractTriggerHandler {
    public ObtainItemTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack pickedUp = event.getItem().getItemStack();
        String itemId = getItemId(pickedUp);
        if (itemId == null) return;
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.OBTAIN_ITEM)) {
            if (!(c.conditions() instanceof AdvancementConditions.ObtainItem(
                    List<String> itemIds, int amount
            ))) continue;
            if (!itemIds.contains(itemId)) continue;
            int total = pickedUp.getAmount();
            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack != null && !stack.getType().isAir() && itemId.equals(getItemId(stack))) {
                    total += stack.getAmount();
                }
            }
            if (total < amount) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
