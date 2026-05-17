package toutouchien.itemsadderadditions.feature.action.listener;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionDispatcher;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

@NullMarked
public final class MiscItemActionListener implements Listener {
    private final ActionDispatcher dispatcher;

    public MiscItemActionListener(ActionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBookWrite(PlayerEditBookEvent event) {
        ItemStack book = event.getSlot() == -1
                ? event.getPlayer().getInventory().getItemInOffHand()
                : event.getPlayer().getInventory().getItem(event.getSlot());
        if (book == null || book.isEmpty()) return;

        dispatchItem(event.getPlayer(), book, TriggerType.ITEM_BOOK_WRITE, builder -> builder);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBookRead(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.WRITTEN_BOOK) return;

        dispatchItem(event.getPlayer(), item, TriggerType.ITEM_BOOK_READ,
                builder -> builder.block(event.getClickedBlock()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onFishing(PlayerFishEvent event) {
        ItemStack rod = event.getHand() == EquipmentSlot.HAND
                ? event.getPlayer().getInventory().getItemInMainHand()
                : event.getPlayer().getInventory().getItemInOffHand();

        CustomStack customRod = CustomStack.byItemStack(rod);
        if (customRod == null) return;

        TriggerType type = switch (event.getState()) {
            case FISHING -> TriggerType.ITEM_FISHING_START;
            case CAUGHT_FISH, CAUGHT_ENTITY -> TriggerType.ITEM_FISHING_CAUGHT;
            case FAILED_ATTEMPT -> TriggerType.ITEM_FISHING_FAILED;
            case REEL_IN -> TriggerType.ITEM_FISHING_CANCEL;
            case BITE -> TriggerType.ITEM_FISHING_BITE;
            case IN_GROUND -> TriggerType.ITEM_FISHING_IN_GROUND;
            case LURED -> null;
        };
        if (type == null) return;

        dispatcher.dispatch(
                customRod.getNamespacedID(),
                type,
                ActionContext.create(event.getPlayer(), type)
                        .heldItem(rod)
                        .build()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        dispatchItem(event.getPlayer(), event.getItemStack(), TriggerType.ITEM_BUCKET_EMPTY,
                builder -> builder.block(event.getBlockClicked()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        dispatchItem(event.getPlayer(), event.getItemStack(), TriggerType.ITEM_BUCKET_FILL,
                builder -> builder.block(event.getBlockClicked()));
    }

    private void dispatchItem(
            org.bukkit.entity.Player player,
            ItemStack item,
            TriggerType type,
            ContextCustomizer customizer
    ) {
        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) return;

        ActionContext.Builder builder = ActionContext.create(player, type).heldItem(item);
        dispatcher.dispatch(customStack.getNamespacedID(), type, customizer.apply(builder).build());
    }

    @FunctionalInterface
    private interface ContextCustomizer {
        ActionContext.Builder apply(ActionContext.Builder builder);
    }
}
