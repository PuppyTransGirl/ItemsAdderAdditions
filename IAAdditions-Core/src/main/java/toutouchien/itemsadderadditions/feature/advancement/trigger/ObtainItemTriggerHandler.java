package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementConditions;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementCriterionDefinition;
import toutouchien.itemsadderadditions.feature.advancement.AdvancementRegistry;

import java.util.List;

@NullMarked
public final class ObtainItemTriggerHandler extends AbstractTriggerHandler {
    private final Plugin plugin;

    public ObtainItemTriggerHandler(AdvancementRegistry registry, Plugin plugin) {
        super(registry);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // The picked-up stack is not in the inventory yet, so count it explicitly.
        checkInventory(player, event.getItem().getItemStack());
    }

    // Items can also enter the inventory through GUI clicks (shift-click, taking from
    // a chest, crafting output) and drags. The inventory only reflects the change on
    // the next tick, so recheck then.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        scheduleRecheck(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        scheduleRecheck(player);
    }

    private void scheduleRecheck(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> checkInventory(player, null));
    }

    private void checkInventory(Player player, @Nullable ItemStack extra) {
        for (AdvancementCriterionDefinition c : registry.criteriaByTrigger(RuntimeTrigger.OBTAIN_ITEM)) {
            if (!(c.conditions() instanceof AdvancementConditions.ObtainItem(
                    List<String> itemIds, int amount
            ))) continue;
            int total = countMatching(player, itemIds);
            if (extra != null && itemIds.stream().anyMatch(id -> matchesItem(extra, id))) {
                total += extra.getAmount();
            }
            if (total < amount) continue;
            award(player, advancementKeyFor(c), c.name());
        }
    }

    private static int countMatching(Player player, List<String> itemIds) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && !stack.getType().isAir()
                    && itemIds.stream().anyMatch(itemId -> matchesItem(stack, itemId))) {
                total += stack.getAmount();
            }
        }
        return total;
    }
}
