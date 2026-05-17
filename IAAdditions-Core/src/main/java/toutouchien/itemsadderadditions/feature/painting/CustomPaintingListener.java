package toutouchien.itemsadderadditions.feature.painting;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class CustomPaintingListener implements Listener {
    private final CustomPaintingManager manager;

    public CustomPaintingListener(CustomPaintingManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.useInteractedBlock() == Event.Result.DENY) return;
        if (event.useItemInHand() == Event.Result.DENY) return;

        EquipmentSlot hand = event.getHand();
        if (hand == null) return;
        if (isDuplicateOffHandPlacement(event, hand)) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        ItemStack item = event.getItem();
        if (item == null || item.isEmpty()) return;

        CustomStack customStack = CustomStack.byItemStack(item);
        if (customStack == null) return;

        CustomPaintingDefinition definition = manager.byItemId(customStack.getNamespacedID());
        if (definition == null) return;

        if (manager.tryPlace(event.getPlayer(), item, hand, clickedBlock, event.getBlockFace(), definition)) {
            event.setCancelled(true);
        }
    }

    private boolean isDuplicateOffHandPlacement(PlayerInteractEvent event, EquipmentSlot hand) {
        if (hand != EquipmentSlot.OFF_HAND) return false;

        ItemStack mainHand = event.getPlayer().getInventory().getItemInMainHand();
        if (mainHand.isEmpty()) return false;

        CustomStack customStack = CustomStack.byItemStack(mainHand);
        return customStack != null && manager.byItemId(customStack.getNamespacedID()) != null;
    }
}
