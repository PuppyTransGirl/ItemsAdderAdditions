package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

@NullMarked
public final class ItemUsedOnBlockTriggerHandler extends AbstractTriggerHandler {
    public ItemUsedOnBlockTriggerHandler(AdvancementRegistry registry) {
        super(registry);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;
        Player player = event.getPlayer();
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.ITEM_USED_ON_BLOCK)) {
            if (!(c.conditions() instanceof AdvancementConditions.ItemUsedOnBlock(String condItemId, String condBlockId))) continue;
            if (!matchesItem(item, condItemId)) continue;
            if (!matchesBlock(block, condBlockId)) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }
}
