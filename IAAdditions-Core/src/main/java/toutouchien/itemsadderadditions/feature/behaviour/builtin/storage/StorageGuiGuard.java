package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

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
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.session.StorageSessionManager;

import java.util.List;
import java.util.Set;

/**
 * Blocks disallowed player insertions into storage GUIs and prevents SHULKER nesting.
 */
@NullMarked
public final class StorageGuiGuard implements Listener {
    private final StorageSessionManager sessions;
    private final Set<String> shulkerItemIDs;
    @Nullable private final List<String> allowedItems;
    @Nullable private final List<String> deniedItems;

    public StorageGuiGuard(
            StorageSessionManager sessions,
            Set<String> shulkerItemIDs,
            @Nullable List<String> allowedItems,
            @Nullable List<String> deniedItems
    ) {
        this.sessions = sessions;
        this.shulkerItemIDs = shulkerItemIDs;
        this.allowedItems = allowedItems;
        this.deniedItems = deniedItems;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (!sessions.ownsInventory(topInv)) return;

        boolean blocked = switch (event.getAction()) {
            case PLACE_ONE, PLACE_SOME, PLACE_ALL, SWAP_WITH_CURSOR -> event.getRawSlot() < topInv.getSize()
                    && blocksInsertion(event.getCursor());

            case MOVE_TO_OTHER_INVENTORY -> event.getRawSlot() >= topInv.getSize()
                    && blocksInsertion(event.getCurrentItem());

            case COLLECT_TO_CURSOR -> isShulker(event.getCursor());

            case HOTBAR_SWAP -> {
                if (event.getRawSlot() >= topInv.getSize()) yield false;
                int slot = event.getHotbarButton();
                ItemStack hotbar = slot >= 0
                        ? player.getInventory().getItem(slot)
                        : player.getInventory().getItemInOffHand();
                yield blocksInsertion(hotbar);
            }

            default -> false;
        };

        if (blocked) {
            event.setCancelled(true);
            Log.debug("StorageGuiGuard", "Blocked {} from inserting an item into a storage GUI.", player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!sessions.ownsInventory(event.getView().getTopInventory())) return;
        if (!blocksInsertion(event.getOldCursor())) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                Log.debug("StorageGuiGuard", "Blocked {} from dragging an item into a storage GUI.", player.getName());
                return;
            }
        }
    }

    private boolean isShulker(@Nullable ItemStack item) {
        String itemId = NamespaceUtils.itemID(item);
        return itemId != null && shulkerItemIDs.contains(itemId);
    }

    private boolean blocksInsertion(@Nullable ItemStack item) {
        String itemId = NamespaceUtils.itemID(item);
        if (itemId == null) return false;
        if (shulkerItemIDs.contains(itemId)) return true;

        if (allowedItems != null) return allowedItems.stream().noneMatch(
                expected -> NamespaceUtils.matchesContentIDOrTag(itemId, expected, CustomTagType.ITEM));
        return deniedItems != null && deniedItems.stream().anyMatch(
                expected -> NamespaceUtils.matchesContentIDOrTag(itemId, expected, CustomTagType.ITEM));
    }
}
