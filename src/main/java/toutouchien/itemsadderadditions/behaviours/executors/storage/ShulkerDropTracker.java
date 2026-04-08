package toutouchien.itemsadderadditions.behaviours.executors.storage;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.behaviours.executors.StorageBehaviour;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks in-flight shulker drops and pre-placement content captures so that
 * {@link StorageBehaviour} does not need to manage these maps directly.
 */
@NullMarked
public final class ShulkerDropTracker implements Listener {
    /**
     * Contents waiting to be injected into a dropping item after a block/furniture break.
     */
    private final Map<BlockCoord, ItemStack[]> pendingShulkerDrops = new HashMap<>();

    /**
     * Contents captured from the hand item before ItemsAdder consumes it on placement.
     * Keyed by player UUID; consumed unconditionally in
     * {@link StorageBehaviour#onFurniturePlaced}.
     */
    private final Map<UUID, ItemStack[]> pendingPlaceContents = new HashMap<>();

    private final String namespacedID;
    private final java.security.Key contentsKey;
    private final org.bukkit.NamespacedKey contentsNamespacedKey;
    private final org.bukkit.NamespacedKey uniqueIdKey;

    public ShulkerDropTracker(
            String namespacedID,
            org.bukkit.NamespacedKey contentsKey,
            org.bukkit.NamespacedKey uniqueIdKey
    ) {
        this.namespacedID = namespacedID;
        this.contentsNamespacedKey = contentsKey;
        this.uniqueIdKey = uniqueIdKey;
        this.contentsKey = null; // unused field - see below
    }

    public void stageDrop(Location loc, ItemStack[] contents) {
        pendingShulkerDrops.put(BlockCoord.of(loc), contents);
        Log.debug("ShulkerDropTracker", "Staged shulker drop at " + loc);
    }

    @Nullable
    public ItemStack[] consumePlaceContents(UUID playerUUID) {
        return pendingPlaceContents.remove(playerUUID);
    }

    public void clear() {
        pendingShulkerDrops.clear();
        pendingPlaceContents.clear();
    }

    /**
     * Pre-captures SHULKER furniture item contents before ItemsAdder consumes the hand item.
     * Must fire at {@link EventPriority#LOWEST} - before ItemsAdder processes the click.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFurniturePlacePreCapture(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        CustomStack cs = CustomStack.byItemStack(hand);
        if (cs == null || !cs.getNamespacedID().equals(namespacedID)) return;

        ItemStack[] stored = StorageInventoryManager.extractFromItem(hand, contentsNamespacedKey);
        if (stored == null) {
            Log.debug("ShulkerDropTracker", "Hand item matched but had no stored contents for " + player.getName());
            return;
        }

        pendingPlaceContents.put(player.getUniqueId(), stored);
        Log.debug("ShulkerDropTracker", "Pre-captured contents for " + player.getName());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDropItem(BlockDropItemEvent event) {
        BlockCoord key = BlockCoord.of(event.getBlock().getLocation());
        ItemStack[] contents = pendingShulkerDrops.get(key);
        if (contents == null) return;

        for (Item droppedItem : event.getItems()) {
            CustomStack cs = CustomStack.byItemStack(droppedItem.getItemStack());
            if (cs == null || !cs.getNamespacedID().equals(namespacedID)) continue;

            pendingShulkerDrops.remove(key);
            injectIfNonEmpty(droppedItem, contents);
            return;
        }
        Log.debug("ShulkerDropTracker", "No matching item in drops yet - waiting for ItemSpawnEvent.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (pendingShulkerDrops.isEmpty()) return;

        BlockCoord spawnKey = BlockCoord.of(event.getEntity().getLocation());
        ItemStack[] contents = pendingShulkerDrops.get(spawnKey);
        if (contents == null) return;

        Item spawnedItem = event.getEntity();
        CustomStack cs = CustomStack.byItemStack(spawnedItem.getItemStack());
        if (cs == null || !cs.getNamespacedID().equals(namespacedID)) return;

        pendingShulkerDrops.remove(spawnKey);
        injectIfNonEmpty(spawnedItem, contents);
    }

    private void injectIfNonEmpty(Item target, ItemStack[] contents) {
        if (!hasAnyContent(contents)) {
            Log.debug("ShulkerDropTracker", "Contents all air - item left clean.");
            return;
        }
        ItemStack stack = target.getItemStack().clone();
        StorageInventoryManager.injectIntoItem(stack, contents, contentsNamespacedKey);
        StorageInventoryManager.stampUniqueId(stack, uniqueIdKey);
        target.setItemStack(stack);
        Log.debug("ShulkerDropTracker", "Injected contents and stamped UID into item.");
    }

    private static boolean hasAnyContent(@Nullable ItemStack[] contents) {
        if (contents == null) return false;
        for (ItemStack item : contents)
            if (item != null && item.getType() != org.bukkit.Material.AIR) return true;
        return false;
    }
}
