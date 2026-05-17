package toutouchien.itemsadderadditions.feature.action.listener;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionDispatcher;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

@NullMarked
public final class ItemInteractionActionListener implements Listener {
    private final ActionDispatcher dispatcher;

    public ItemInteractionActionListener(ActionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        String argument = ActionEventFilters.interactArgument(event);
        if (argument == null) return;
        if (!ActionEventFilters.isInteractAllowed(event)) return;
        if (ActionEventFilters.ignoreOffHandDuplicate(event)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) return;

        dispatchGenericAndHandSpecific(
                customStack.getNamespacedID(),
                event.getPlayer(),
                event.getHand(),
                argument,
                item,
                event.getClickedBlock(),
                null
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        if (ActionEventFilters.ignoreOffHandDuplicate(player, hand)) return;

        ItemStack item = hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) return;

        dispatchGenericAndHandSpecific(
                customStack.getNamespacedID(),
                player,
                hand,
                "entity",
                item,
                null,
                event.getRightClicked()
        );
    }

    private void dispatchGenericAndHandSpecific(
            String namespacedId,
            Player player,
            EquipmentSlot hand,
            String argument,
            ItemStack item,
            @Nullable Block clickedBlock,
            @Nullable Entity target
    ) {
        dispatcher.dispatch(
                namespacedId,
                TriggerType.ITEM_INTERACT,
                argument,
                ActionContext.create(player, TriggerType.ITEM_INTERACT)
                        .block(clickedBlock)
                        .target(target)
                        .heldItem(item)
                        .eventArgument(argument)
                        .build()
        );

        TriggerType handType = hand == EquipmentSlot.HAND
                ? TriggerType.ITEM_INTERACT_MAINHAND
                : TriggerType.ITEM_INTERACT_OFFHAND;

        dispatcher.dispatch(
                namespacedId,
                handType,
                argument,
                ActionContext.create(player, handType)
                        .block(clickedBlock)
                        .target(target)
                        .heldItem(item)
                        .eventArgument(argument)
                        .build()
        );
    }
}
