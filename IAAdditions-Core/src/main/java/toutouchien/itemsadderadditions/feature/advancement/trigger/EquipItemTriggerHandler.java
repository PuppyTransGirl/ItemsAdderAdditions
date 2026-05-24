package toutouchien.itemsadderadditions.feature.advancement.trigger;

import io.papermc.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

import java.util.Locale;

@NullMarked
public final class EquipItemTriggerHandler extends AbstractTriggerHandler {
    public EquipItemTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        ItemStack newItem = event.getNewItem();
        if (newItem.getType().isAir()) return;
        Player player = event.getPlayer();
        String itemId = getItemId(newItem);
        if (itemId == null) return;
        String slot = event.getSlotType().name().toLowerCase(Locale.ROOT);
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.EQUIP_ITEM)) {
            if (!(c.conditions() instanceof AdvancementConditions.EquipItem(String condItemId, String condSlot))) continue;
            if (condItemId != null && !condItemId.equals(itemId)) continue;
            if (condSlot != null && !condSlot.equalsIgnoreCase(slot)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
