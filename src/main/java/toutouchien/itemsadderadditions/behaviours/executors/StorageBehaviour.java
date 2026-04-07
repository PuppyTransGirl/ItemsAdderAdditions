package toutouchien.itemsadderadditions.behaviours.executors;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.behaviours.executors.storage.StorageInventoryHolder;
import toutouchien.itemsadderadditions.behaviours.executors.storage.StorageInventoryManager;
import toutouchien.itemsadderadditions.behaviours.executors.storage.StorageSession;
import toutouchien.itemsadderadditions.behaviours.executors.storage.StorageType;
import toutouchien.itemsadderadditions.utils.other.ItemCategory;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.*;

@SuppressWarnings("unused")
@NullMarked
@Behaviour(key = "storage")
public final class StorageBehaviour extends BehaviourExecutor implements Listener {
    private static final Set<String> SHULKER_ITEM_IDS = Collections.synchronizedSet(new HashSet<>());
    private static final Set<Inventory> OPEN_STORAGE_INVENTORIES = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    private final Map<UUID, StorageSession> openSessions = new HashMap<>();
    private final Map<Location, ItemStack[]> pendingShulkerDrops = new HashMap<>();
    private final Map<Location, ItemStack[]> preloadedBlockContents = new HashMap<>();

    @Parameter(key = "type", type = String.class, required = true)
    private String typeName;

    @Parameter(key = "rows", type = Integer.class, min = 1, max = 6)
    private int rows = 3;

    @Parameter(key = "title", type = String.class)
    @Nullable private String titleRaw;

    private StorageType storageType = StorageType.STORAGE;
    private String namespacedID = "";
    private ItemCategory category = ItemCategory.BLOCK;
    private NamespacedKey contentsKey;
    private NamespacedKey uniqueIdKey;
    private JavaPlugin plugin;
    private Component title = Component.empty();

    private static void dropItems(Location loc, @Nullable ItemStack[] contents) {
        if (contents == null) {
            Log.debug("Storage", "[DropItems] Contents array is null, nothing to drop.");
            return;
        }

        int droppedCount = 0;
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                loc.getWorld().dropItemNaturally(loc, item);
                droppedCount++;
            }
        }
        Log.debug("Storage", "[DropItems] Successfully dropped " + droppedCount + " items.");
    }

    @Nullable
    private static ItemStack[] extractAndClearFromHand(Player player, NamespacedKey key) {
        PlayerInventory inv = player.getInventory();
        // Survival: ItemsAdder consumed the item before CustomBlockPlaceEvent fired - nothing to write back.
        // Creative: the user explicitly wants PDC data preserved on the item - also nothing to write back.
        // Either way: just read and return; never modify the hand item here.

        ItemStack[] stored = StorageInventoryManager.extractFromItem(inv.getItemInMainHand(), key);
        if (stored != null)
            return stored;

        stored = StorageInventoryManager.extractFromItem(inv.getItemInOffHand(), key);
        return stored;
    }

    /**
     * Returns {@code true} if {@code contents} contains at least one non-air, non-null slot.
     * Used to decide whether to inject PDC data and a unique ID into a dropped item.
     */
    private static boolean hasAnyContent(@Nullable ItemStack[] contents) {
        if (contents == null)
            return false;

        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR)
                return true;
        }

        return false;
    }

    private static boolean isShulkerStorageItem(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return false;

        CustomStack cs = CustomStack.byItemStack(item);
        return cs != null && SHULKER_ITEM_IDS.contains(cs.getNamespacedID());
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        this.namespacedID = host.namespacedID();
        this.category = host.category();
        this.plugin = host.plugin();
        this.contentsKey = new NamespacedKey(plugin, "storage_" + namespacedID.replace(":", "_"));
        this.uniqueIdKey = new NamespacedKey(plugin, "storage_uid_" + namespacedID.replace(":", "_"));

        try {
            storageType = StorageType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Log.warn("Storage", "Unknown storage type '{}' for '{}'. Valid: STORAGE, SHULKER, DISPOSAL. Defaulting to STORAGE.", typeName, namespacedID);
        }

        Component component = CustomStack.getInstance(namespacedID).itemName();
        if (component.color() == NamedTextColor.WHITE)
            component = component.color(NamedTextColor.DARK_GRAY);

        this.title = (titleRaw != null)
                ? MiniMessage.miniMessage().deserialize(titleRaw)
                : component;

        StorageInventoryManager.ensureCustomBlockDataRegistered(plugin);
        if (storageType == StorageType.SHULKER)
            SHULKER_ITEM_IDS.add(namespacedID);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        Log.debug("Storage", "Loaded StorageBehaviour for " + namespacedID + " as type " + storageType);
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        Set<Inventory> flushed = new HashSet<>();
        for (StorageSession session : openSessions.values()) {
            if (!flushed.contains(session.inventory())) {
                saveSessionContents(session);
                flushed.add(session.inventory());
            }

            session.player().closeInventory();
        }

        openSessions.clear();
        pendingShulkerDrops.clear();
        preloadedBlockContents.clear();
        SHULKER_ITEM_IDS.remove(namespacedID);

        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        if (event.getHand() != EquipmentSlot.HAND)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        if (category != ItemCategory.BLOCK)
            return;

        if (event.getPlayer().isSneaking())
            return;

        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        if (customBlock == null || !customBlock.getNamespacedID().equals(namespacedID))
            return;

        event.setCancelled(true);
        openForBlock(event.getPlayer(), block);
    }

    /**
     * Pre-loads block contents at HIGH priority, before CustomBlockData's MONITOR-priority
     * BlockBreakEvent listener automatically deletes PDC data for broken blocks.
     * The loaded contents are cached in {@code preloadedBlockContents} and consumed by
     * {@link #onBlockBreak(CustomBlockBreakEvent)}.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVanillaBlockBreakPreLoad(BlockBreakEvent event) {
        if (category != ItemCategory.BLOCK)
            return;

        Block block = event.getBlock();
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        if (customBlock == null || !customBlock.getNamespacedID().equals(namespacedID))
            return;

        ItemStack[] contents = StorageInventoryManager.loadFromBlock(block, contentsKey, plugin);
        Log.debug("Storage", "[BlockBreakPre] Pre-loaded contents before CustomBlockData MONITOR cleanup. Is null? " + (contents == null));
        if (contents != null)
            preloadedBlockContents.put(block.getLocation().clone(), contents);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(CustomBlockBreakEvent event) {
        if (!event.getNamespacedID().equals(namespacedID))
            return;

        Block block = event.getBlock();
        Log.debug("Storage", "[BlockBreak] CustomBlockBreakEvent triggered for: " + namespacedID + " at " + block.getLocation());

        closeSessionsAt(block.getLocation());

        // Contents were pre-loaded in onVanillaBlockBreakPreLoad() before CustomBlockData's
        // MONITOR listener deleted them. Fetch from cache here.
        ItemStack[] contents = preloadedBlockContents.remove(block.getLocation());
        Log.debug("Storage", "[BlockBreak] Retrieved pre-loaded contents from cache. Is null? " + (contents == null));

        switch (storageType) {
            case STORAGE -> {
                Log.debug("Storage", "[BlockBreak:STORAGE] Dropping contents. Is null? " + (contents == null));
                dropItems(block.getLocation(), contents);
            }

            case SHULKER -> {
                Log.debug("Storage", "[BlockBreak:SHULKER] Loaded contents for pending drop. Is null? " + (contents == null));
                if (contents != null) {
                    pendingShulkerDrops.put(block.getLocation().clone(), contents);
                    Log.debug("Storage", "[BlockBreak:SHULKER] Saved pending shulker drop at " + block.getLocation());
                }
            }
        }

        // Data was already cleared by CustomBlockData's MONITOR listener; this is a no-op safety call.
        StorageInventoryManager.clearBlock(block, plugin);
        Log.debug("Storage", "[BlockBreak] Cleared block PDC data at " + block.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(CustomBlockPlaceEvent event) {
        if (!event.getNamespacedID().equals(namespacedID))
            return;

        if (storageType != StorageType.SHULKER)
            return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        Log.debug("Storage", "[BlockPlace:SHULKER] Block placed. Checking hand for contents...");
        ItemStack[] stored = extractAndClearFromHand(player, contentsKey);
        if (stored == null) {
            Log.debug("Storage", "[BlockPlace:SHULKER] Hand item had no stored contents.");
            return;
        }

        StorageInventoryManager.saveToBlock(block, stored, contentsKey, plugin);
        Log.debug("Storage", "[BlockPlace:SHULKER] Successfully restored contents into placed block.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Location loc = event.getBlock().getLocation();
        ItemStack[] contents = pendingShulkerDrops.get(loc);
        if (contents == null)
            return;

        Log.debug("Storage", "[BlockDropItem] Found pending shulker drop for location " + loc);

        for (Item droppedItem : event.getItems()) {
            CustomStack cs = CustomStack.byItemStack(droppedItem.getItemStack());
            if (cs != null && cs.getNamespacedID().equals(namespacedID)) {
                pendingShulkerDrops.remove(loc);
                if (hasAnyContent(contents)) {
                    ItemStack newStack = droppedItem.getItemStack().clone();
                    StorageInventoryManager.injectIntoItem(newStack, contents, contentsKey);
                    StorageInventoryManager.stampUniqueId(newStack, uniqueIdKey);
                    droppedItem.setItemStack(newStack);
                    Log.debug("Storage", "[BlockDropItem] Injected contents and stamped UID into dropped item.");
                } else {
                    Log.debug("Storage", "[BlockDropItem] Contents are all air; dropped item left clean (no PDC data).");
                }
                return;
            }
        }
        Log.debug("Storage", "[BlockDropItem] No matching ItemsAdder custom item found in drops. Left in pending map.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        if (!event.getNamespacedID().equals(namespacedID))
            return;

        if (category != ItemCategory.FURNITURE && category != ItemCategory.COMPLEX_FURNITURE)
            return;

        if (event.getPlayer().isSneaking())
            return;

        event.setCancelled(true);
        openForEntity(event.getPlayer(), event.getBukkitEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        if (!event.getNamespacedID().equals(namespacedID))
            return;

        Entity entity = event.getBukkitEntity();
        Log.debug("Storage", "[FurnitureBreak] FurnitureBreakEvent triggered for: " + namespacedID);

        closeSessionsAt(entity.getLocation());

        switch (storageType) {
            case STORAGE -> {
                ItemStack[] contents = StorageInventoryManager.loadFromEntity(entity, contentsKey);
                Log.debug("Storage", "[FurnitureBreak:STORAGE] Loaded contents to drop. Is null? " + (contents == null));
                dropItems(entity.getLocation(), contents);
            }
            case SHULKER -> {
                ItemStack[] contents = StorageInventoryManager.loadFromEntity(entity, contentsKey);
                Log.debug("Storage", "[FurnitureBreak:SHULKER] Loaded contents for pending drop. Is null? " + (contents == null));
                if (contents != null) {
                    pendingShulkerDrops.put(entity.getLocation().clone(), contents);
                    Log.debug("Storage", "[FurnitureBreak:SHULKER] Saved pending shulker drop at " + entity.getLocation());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlaced(FurniturePlacedEvent event) {
        if (!event.getNamespacedID().equals(namespacedID))
            return;

        if (storageType != StorageType.SHULKER)
            return;

        Player player = event.getPlayer();
        Entity entity = event.getBukkitEntity();

        Log.debug("Storage", "[FurniturePlace:SHULKER] Furniture placed. Checking hand for contents...");
        ItemStack[] stored = extractAndClearFromHand(player, contentsKey);
        if (stored == null) {
            Log.debug("Storage", "[FurniturePlace:SHULKER] Hand item had no stored contents.");
            return;
        }

        StorageInventoryManager.saveToEntity(entity, stored, contentsKey);
        Log.debug("Storage", "[FurniturePlace:SHULKER] Successfully restored contents into placed furniture.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (pendingShulkerDrops.isEmpty())
            return;

        Location spawnLoc = event.getEntity().getLocation();
        Location matchKey = null;

        for (Location loc : pendingShulkerDrops.keySet()) {
            if (loc.getWorld() != null && loc.getWorld().equals(spawnLoc.getWorld()) && loc.distanceSquared(spawnLoc) <= 9.0) {
                matchKey = loc;
                break;
            }
        }
        if (matchKey == null)
            return;

        Item spawnedItem = event.getEntity();
        CustomStack cs = CustomStack.byItemStack(spawnedItem.getItemStack());
        if (cs == null || !cs.getNamespacedID().equals(namespacedID))
            return;

        Log.debug("Storage", "[ItemSpawn] Found matching spawned item for pending shulker drop.");

        ItemStack[] contents = pendingShulkerDrops.remove(matchKey);
        if (contents == null)
            return;

        if (hasAnyContent(contents)) {
            ItemStack newStack = spawnedItem.getItemStack().clone();
            StorageInventoryManager.injectIntoItem(newStack, contents, contentsKey);
            StorageInventoryManager.stampUniqueId(newStack, uniqueIdKey);
            spawnedItem.setItemStack(newStack);
            Log.debug("Storage", "[ItemSpawn] Injected contents and stamped UID into spawned furniture item.");
        } else {
            Log.debug("Storage", "[ItemSpawn] Contents are all air; spawned item left clean (no PDC data).");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;

        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder(false) instanceof StorageInventoryHolder holder))
            return;

        Block block = holder.location().getBlock();
        ItemStack[] contents = inventory.getContents();

        StorageInventoryManager.saveToBlock(block, contents, contentsKey, plugin);
    }

    private void openForBlock(Player player, Block block) {
        Log.debug("Storage", "[OpenGUI] Opening block storage for " + player.getName());
        Inventory inv = resolveInventory(block.getLocation(), player, block, null);
        openSessions.put(player.getUniqueId(), new StorageSession(player, inv, block, null, storageType));
        OPEN_STORAGE_INVENTORIES.add(inv);
        player.openInventory(inv);
    }

    private void openForEntity(Player player, Entity entity) {
        Log.debug("Storage", "[OpenGUI] Opening furniture storage for " + player.getName());
        Inventory inv = resolveInventory(entity.getLocation(), player, null, entity);
        openSessions.put(player.getUniqueId(), new StorageSession(player, inv, null, entity, storageType));
        OPEN_STORAGE_INVENTORIES.add(inv);
        player.openInventory(inv);
    }

    private Inventory resolveInventory(Location loc, Player player, @Nullable Block block, @Nullable Entity entity) {
        if (storageType == StorageType.STORAGE || storageType == StorageType.SHULKER) {
            Inventory live = findLiveSharedInventory(loc);
            if (live != null) {
                Log.debug("Storage", "[ResolveInv] Returning existing live inventory instance.");
                return live;
            }
        }

        StorageInventoryHolder holder = new StorageInventoryHolder(block.getLocation());
        Inventory inv = Bukkit.createInventory(holder, rows * 9, title);
        holder.inventory(inv);

        ItemStack[] stored = switch (storageType) {
            case STORAGE, SHULKER -> block != null
                    ? StorageInventoryManager.loadFromBlock(block, contentsKey, plugin)
                    : StorageInventoryManager.loadFromEntity(entity, contentsKey);

            case DISPOSAL -> null;
        };

        StorageInventoryManager.populateInventory(inv, stored);
        Log.debug("Storage", "[ResolveInv] Created new inventory instance. Populated items? " + (stored != null));
        return inv;
    }

    @Nullable
    private Inventory findLiveSharedInventory(Location loc) {
        for (StorageSession s : openSessions.values()) {
            if (s.type() != StorageType.STORAGE && s.type() != StorageType.SHULKER)
                continue;

            Location holderLoc = s.holderLocation();
            if (holderLoc.getWorld() != null && holderLoc.getWorld().equals(loc.getWorld()) && holderLoc.distanceSquared(loc) < 0.01)
                return s.inventory();
        }

        return null;
    }

    private boolean isLastViewer(StorageSession closedSession) {
        for (StorageSession s : openSessions.values()) {
            if (s.inventory() == closedSession.inventory())
                return false;
        }

        return true;
    }

    private void saveSessionContents(StorageSession session) {
        ItemStack[] contents = session.inventory().getContents();

        if (session.isBlock()) {
            StorageInventoryManager.saveToBlock(session.block(), contents, contentsKey, plugin);
            Log.debug("Storage", "[SaveContents] Saved block inventory to PDC.");
        } else if (session.isFurniture()) {
            StorageInventoryManager.saveToEntity(session.entity(), contents, contentsKey);
            Log.debug("Storage", "[SaveContents] Saved furniture inventory to PDC.");
        }
    }

    private void closeSessionsAt(Location loc) {
        Set<Inventory> alreadySaved = new HashSet<>();
        Iterator<Map.Entry<UUID, StorageSession>> it = openSessions.entrySet().iterator();

        while (it.hasNext()) {
            StorageSession session = it.next().getValue();
            Location holderLoc = session.holderLocation();

            boolean sameWorld = holderLoc.getWorld() != null && holderLoc.getWorld().equals(loc.getWorld());
            boolean nearby = sameWorld && holderLoc.distanceSquared(loc) <= 2.25;
            if (!nearby)
                continue;

            if (session.type() != StorageType.DISPOSAL && !alreadySaved.contains(session.inventory())) {
                Log.debug("Storage", "[ForceClose] Forcing save due to block/entity break nearby.");
                saveSessionContents(session);
                alreadySaved.add(session.inventory());
                OPEN_STORAGE_INVENTORIES.remove(session.inventory());
                // The pre-loaded snapshot captured in onVanillaBlockBreakPreLoad may now be stale
                // (e.g. a player removed items from the GUI before the break).  Overwrite it with
                // the contents we just saved so the shulker drop reflects the real current state.
                if (session.isBlock() && session.block() != null) {
                    preloadedBlockContents.put(session.block().getLocation().clone(), session.inventory().getContents());
                    Log.debug("Storage", "[ForceClose] Updated preload cache with current GUI state.");
                }
            }

            // Must happen BEFORE closeInventory(): that call fires InventoryCloseEvent
            // synchronously, which would invoke onInventoryClose -> openSessions.remove() while we
            // are still iterating - causing a ConcurrentModificationException.  Removing first
            // means onInventoryClose finds no session for this player and returns immediately.
            it.remove();
            session.player().closeInventory();
        }
    }

    private static final EnumSet<InventoryAction> SHULKER_BLOCKED_ACTIONS = EnumSet.of(
            InventoryAction.PLACE_ONE,
            InventoryAction.PLACE_SOME,
            InventoryAction.PLACE_ALL,
            InventoryAction.SWAP_WITH_CURSOR,
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.COLLECT_TO_CURSOR
    );

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        if (!(event.getView().getTopInventory().getHolder(false) instanceof StorageInventoryHolder))
            return;

        Inventory topInv = event.getView().getTopInventory();

        boolean blocked = switch (event.getAction()) {
            case PLACE_ONE, PLACE_SOME, PLACE_ALL, SWAP_WITH_CURSOR ->
                    event.getRawSlot() < topInv.getSize() && isShulkerStorageItem(event.getCursor());

            case MOVE_TO_OTHER_INVENTORY ->
                    event.getRawSlot() >= topInv.getSize() && isShulkerStorageItem(event.getCurrentItem());

            case COLLECT_TO_CURSOR ->
                    isShulkerStorageItem(event.getCursor());

            case HOTBAR_SWAP -> {
                if (event.getRawSlot() >= topInv.getSize())
                    yield false;

                int hotbarSlot = event.getHotbarButton();
                ItemStack hotbarItem = hotbarSlot >= 0
                        ? player.getInventory().getItem(hotbarSlot)
                        : player.getInventory().getItemInOffHand();
                yield isShulkerStorageItem(hotbarItem);
            }
            default -> false;
        };

        if (blocked) {
            event.setCancelled(true);
            Log.debug("Storage", "[NestGuard] Blocked " + player.getName() + " from placing a SHULKER storage item inside a storage GUI.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        if (!(event.getView().getTopInventory().getHolder(false) instanceof StorageInventoryHolder))
            return;

        if (!isShulkerStorageItem(event.getOldCursor()))
            return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                Log.debug("Storage", "[NestGuard] Blocked " + player.getName() + " from dragging a SHULKER storage item into a storage GUI.");
                return;
            }
        }
    }
}
