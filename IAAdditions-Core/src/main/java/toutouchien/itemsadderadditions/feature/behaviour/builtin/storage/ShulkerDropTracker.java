package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.common.utils.BlockCoord;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.StorageBehaviour;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryManager;

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
     * Chebyshev block radius for matching a freshly spawned item to a staged drop. IA drops the
     * furniture item with {@code World#dropItemNaturally}, which nudges the location away from
     * solid blocks, so the item rarely spawns in the exact furniture block.
     */
    private static final int MATCH_RADIUS = 2;
    private static final int CREATIVE_FALLBACK_MAX_TICKS_LIVED = 2;

    /**
     * Contents waiting to be injected into a dropping item after a block/furniture break.
     */
    private final Map<BlockCoord, ItemStack[]> pendingShulkerDrops = new HashMap<>();

    /**
     * Contents captured from the hand item before ItemsAdder consumes it on placement.
     * Keyed by player UUID
     */
    private final Map<UUID, ItemStack[]> pendingPlaceContents = new HashMap<>();

    private final JavaPlugin plugin;
    private final String namespacedID;
    private final org.bukkit.NamespacedKey contentsNamespacedKey;
    private final org.bukkit.NamespacedKey uniqueIdKey;

    public ShulkerDropTracker(
            JavaPlugin plugin,
            String namespacedID,
            org.bukkit.NamespacedKey contentsKey,
            org.bukkit.NamespacedKey uniqueIdKey
    ) {
        this.plugin = plugin;
        this.namespacedID = namespacedID;
        this.contentsNamespacedKey = contentsKey;
        this.uniqueIdKey = uniqueIdKey;
    }

    private static boolean hasAnyContent(@Nullable ItemStack[] contents) {
        if (contents == null) return false;
        for (ItemStack item : contents)
            if (item != null && item.getType() != org.bukkit.Material.AIR) return true;
        return false;
    }

    public void stageDrop(Location loc, ItemStack[] contents) {
        stageDrop(loc, contents, false);
    }

    public void stageCreativeDrop(Location loc, ItemStack[] contents) {
        stageDrop(loc, contents, true);
    }

    private void stageDrop(Location loc, ItemStack[] contents, boolean createPortableItemFallback) {
        BlockCoord key = BlockCoord.of(loc);
        pendingShulkerDrops.put(key, contents);
        Log.debug("ShulkerDropTracker", "Staged shulker drop at " + loc);

        // Fallback: if no spawn/drop event claimed the staged contents within a couple of ticks,
        // search for IA's dropped item nearby and write the contents into it. Survival scatters
        // contents as a last resort; creative creates the portable item because IA usually drops
        // nothing there.
        long fallbackDelay = createPortableItemFallback ? 1L : 3L;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ItemStack[] unconsumed = pendingShulkerDrops.remove(key);
            if (unconsumed == null) return; // already consumed by a spawn/drop event

            World world = loc.getWorld();
            if (world == null) return;

            Log.debug("ShulkerDropTracker", "No spawn event claimed staged drop - searching nearby.");
            for (Entity e : world.getNearbyEntities(loc, MATCH_RADIUS, MATCH_RADIUS, MATCH_RADIUS)) {
                if (!(e instanceof Item droppedItem)) continue;
                if (createPortableItemFallback
                        && droppedItem.getTicksLived() > CREATIVE_FALLBACK_MAX_TICKS_LIVED) continue;
                CustomStack cs = CustomStack.byItemStack(droppedItem.getItemStack());
                if (cs == null || !matchesId(cs.getNamespacedID())) continue;

                applyContents(droppedItem, unconsumed);
                Log.debug("ShulkerDropTracker", "Applied contents to nearby dropped item via fallback.");
                return;
            }

            if (createPortableItemFallback) {
                dropPortableItem(loc, unconsumed);
                return;
            }

            scatterContents(world, loc, unconsumed);
        }, fallbackDelay);
    }

    public void dropPortableItem(Location loc, @Nullable ItemStack[] contents) {
        if (!hasAnyContent(contents)) {
            Log.debug("ShulkerDropTracker", "Creative shulker break had no live contents - no portable item fallback needed.");
            return;
        }

        World world = loc.getWorld();
        if (world == null) return;

        CustomStack original;
        try {
            original = CustomStack.getInstance(namespacedID);
        } catch (RuntimeException e) {
            Log.warn("ShulkerDropTracker", "Could not resolve portable storage item '{}' after creative break. Scattering contents instead. Cause: {}",
                    namespacedID, e.getMessage());
            scatterContents(world, loc, contents);
            return;
        }

        if (original == null) {
            Log.warn("ShulkerDropTracker", "Could not resolve portable storage item '{}' after creative break. Scattering contents instead.",
                    namespacedID);
            scatterContents(world, loc, contents);
            return;
        }

        ItemStack drop = original.getItemStack();
        StorageInventoryManager.injectIntoItem(drop, contents, contentsNamespacedKey);
        StorageInventoryManager.stampUniqueId(drop, uniqueIdKey);
        world.dropItemNaturally(loc, drop);
        Log.debug("ShulkerDropTracker", "Dropped portable storage item with live contents for creative break.");
    }

    private void scatterContents(World world, Location loc, @Nullable ItemStack[] contents) {
        if (hasAnyContent(contents)) {
            Log.debug("ShulkerDropTracker", "No dropped item found - scattering contents.");
            for (ItemStack item : contents)
                if (item != null && item.getType() != org.bukkit.Material.AIR)
                    world.dropItemNaturally(loc, item);
        }
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
        if (pendingShulkerDrops.isEmpty()) return;

        for (Item droppedItem : event.getItems()) {
            CustomStack cs = CustomStack.byItemStack(droppedItem.getItemStack());
            if (cs == null || !matchesId(cs.getNamespacedID())) continue;

            BlockCoord key = findStaged(droppedItem.getLocation());
            if (key == null) continue;

            ItemStack[] contents = pendingShulkerDrops.remove(key);
            applyContents(droppedItem, contents);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (pendingShulkerDrops.isEmpty()) return;

        Item spawnedItem = event.getEntity();
        CustomStack cs = CustomStack.byItemStack(spawnedItem.getItemStack());
        if (cs == null || !matchesId(cs.getNamespacedID())) return;

        BlockCoord key = findStaged(spawnedItem.getLocation());
        if (key == null) return;

        ItemStack[] contents = pendingShulkerDrops.remove(key);
        applyContents(spawnedItem, contents);
    }

    private boolean matchesId(String id) {
        return NamespaceUtils.matchesWithRotation(id, namespacedID);
    }

    /**
     * Finds a staged drop whose block is at, or within {@link #MATCH_RADIUS} blocks of,
     * {@code loc}. Prefers an exact block match before falling back to the nearest one.
     */
    @Nullable
    private BlockCoord findStaged(Location loc) {
        BlockCoord exact = BlockCoord.of(loc);
        if (pendingShulkerDrops.containsKey(exact)) return exact;

        String world = loc.getWorld() == null ? "" : loc.getWorld().getName();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        for (BlockCoord key : pendingShulkerDrops.keySet()) {
            if (!key.world().equals(world)) continue;
            if (Math.abs(key.x() - bx) <= MATCH_RADIUS
                    && Math.abs(key.y() - by) <= MATCH_RADIUS
                    && Math.abs(key.z() - bz) <= MATCH_RADIUS)
                return key;
        }
        return null;
    }

    /**
     * Overwrites the dropped item's stored data with {@code contents}: injects and stamps a fresh
     * unique id when there is anything to store, otherwise strips any stale data so the item drops
     * clean. Overwriting (rather than skipping items that already carry data) is what prevents the
     * duplicate-with-contents drop when IA re-drops the item it cached at placement time.
     */
    private void applyContents(Item target, @Nullable ItemStack[] contents) {
        ItemStack stack = target.getItemStack().clone();
        if (hasAnyContent(contents)) {
            StorageInventoryManager.injectIntoItem(stack, contents, contentsNamespacedKey);
            StorageInventoryManager.stampUniqueId(stack, uniqueIdKey);
            Log.debug("ShulkerDropTracker", "Wrote live contents into dropped item.");
        } else {
            stripStoredData(stack);
            Log.debug("ShulkerDropTracker", "Live contents empty - dropped item left clean.");
        }
        target.setItemStack(stack);
    }

    private void stripStoredData(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().remove(contentsNamespacedKey);
        meta.getPersistentDataContainer().remove(uniqueIdKey);
        item.setItemMeta(meta);
    }
}
