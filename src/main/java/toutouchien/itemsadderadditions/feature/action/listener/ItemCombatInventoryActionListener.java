package toutouchien.itemsadderadditions.feature.action.listener;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionDispatcher;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

@NullMarked
public final class ItemCombatInventoryActionListener implements Listener {
    private final ActionDispatcher dispatcher;

    public ItemCombatInventoryActionListener(ActionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        CustomStack customTool = CustomStack.byItemStack(tool);
        if (customTool == null) return;

        dispatcher.dispatch(
                customTool.getNamespacedID(),
                TriggerType.ITEM_ATTACK,
                ActionContext.create(player, TriggerType.ITEM_ATTACK)
                        .target(event.getEntity())
                        .heldItem(tool)
                        .build()
        );
    }

    @EventHandler
    public void onItemKill(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;

        ItemStack tool = killer.getInventory().getItemInMainHand();
        CustomStack customTool = CustomStack.byItemStack(tool);
        if (customTool == null) return;

        dispatcher.dispatch(
                customTool.getNamespacedID(),
                TriggerType.ITEM_KILL,
                ActionContext.create(killer, TriggerType.ITEM_KILL)
                        .target(dead)
                        .heldItem(tool)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        dispatchItem(event.getPlayer(), item, TriggerType.ITEM_DROP);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        dispatchItem(player, event.getItem().getItemStack(), TriggerType.ITEM_PICKUP);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        ItemStack leaving = player.getInventory().getItem(event.getPreviousSlot());
        dispatchIfPresent(player, leaving, TriggerType.ITEM_UNHELD);

        ItemStack entering = player.getInventory().getItem(event.getNewSlot());
        dispatchIfPresent(player, entering, TriggerType.ITEM_HELD);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        dispatchIfPresent(player, event.getMainHandItem(), TriggerType.ITEM_HELD_OFFHAND);
        dispatchIfPresent(player, event.getOffHandItem(), TriggerType.ITEM_UNHELD_OFFHAND);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent event) {
        dispatchItem(event.getPlayer(), event.getBrokenItem(), TriggerType.ITEM_BREAK);
    }

    private void dispatchIfPresent(Player player, ItemStack item, TriggerType type) {
        if (item == null || item.isEmpty()) {
            return;
        }
        dispatchItem(player, item, type);
    }

    private void dispatchItem(Player player, ItemStack item, TriggerType type) {
        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) return;

        dispatcher.dispatch(
                customStack.getNamespacedID(),
                type,
                ActionContext.create(player, type)
                        .heldItem(item)
                        .build()
        );
    }
}
