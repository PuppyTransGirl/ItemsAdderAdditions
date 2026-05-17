package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.Set;

/**
 * Prevents SHULKER-type storage items from being placed inside any storage GUI,
 * blocking nesting.
 */
@NullMarked
public final class StorageGuiGuard implements Listener {
    private final Set<String> shulkerItemIDs;

    public StorageGuiGuard(Set<String> shulkerItemIDs) {
        this.shulkerItemIDs = shulkerItemIDs;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder(false)
                instanceof StorageInventoryHolder)) return;

        Inventory topInv = event.getView().getTopInventory();

        boolean blocked = switch (event.getAction()) {
            case PLACE_ONE, PLACE_SOME, PLACE_ALL, SWAP_WITH_CURSOR -> event.getRawSlot() < topInv.getSize()
                    && isShulker(event.getCursor());

            case MOVE_TO_OTHER_INVENTORY -> event.getRawSlot() >= topInv.getSize()
                    && isShulker(event.getCurrentItem());

            case COLLECT_TO_CURSOR -> isShulker(event.getCursor());

            case HOTBAR_SWAP -> {
                if (event.getRawSlot() >= topInv.getSize()) yield false;
                int slot = event.getHotbarButton();
                ItemStack hotbar = slot >= 0
                        ? player.getInventory().getItem(slot)
                        : player.getInventory().getItemInOffHand();
                yield isShulker(hotbar);
            }

            default -> false;
        };

        if (blocked) {
            event.setCancelled(true);
            Log.debug("StorageGuiGuard",
                    "Blocked {} from placing a SHULKER storage item inside a storage GUI.",
                    player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder(false)
                instanceof StorageInventoryHolder)) return;
        if (!isShulker(event.getOldCursor())) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                Log.debug("StorageGuiGuard",
                        "Blocked {} from dragging a SHULKER storage item into a storage GUI.",
                        player.getName());
                return;
            }
        }
    }

    private boolean isShulker(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        CustomStack cs = CustomStack.byItemStack(item);
        return cs != null && shulkerItemIDs.contains(cs.getNamespacedID());
    }
}
