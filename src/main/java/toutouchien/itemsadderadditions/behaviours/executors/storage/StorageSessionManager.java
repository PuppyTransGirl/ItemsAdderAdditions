package toutouchien.itemsadderadditions.behaviours.executors.storage;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.hook.CoreProtectUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.*;

/**
 * Manages open storage GUI sessions: opening, sharing, saving, and force-closing.
 */
@NullMarked
public final class StorageSessionManager {
    private final Map<UUID, StorageSession> openSessions = new HashMap<>();

    private final int rows;
    private final Component title;
    private final StorageType storageType;
    private final NamespacedKey contentsKey;
    private final JavaPlugin plugin;
    @Nullable private final Sound openSound;
    @Nullable private final Sound closeSound;

    public StorageSessionManager(
            int rows,
            Component title,
            StorageType storageType,
            NamespacedKey contentsKey,
            JavaPlugin plugin,
            Sound openSound,
            Sound closeSound
    ) {
        this.rows = rows;
        this.title = title;
        this.storageType = storageType;
        this.contentsKey = contentsKey;
        this.plugin = plugin;
        this.openSound = openSound;
        this.closeSound = closeSound;
    }

    public void openForBlock(Player player, Block block) {
        Inventory inv = resolveInventory(block.getLocation(), block, null);
        openSessions.put(player.getUniqueId(), new StorageSession(player, inv, block, null, storageType));
        player.openInventory(inv);
        CoreProtectUtils.logInteraction(player.getName(), block.getLocation());

        executeOpen(block.getLocation(), true);
    }

    public void openForEntity(Player player, Entity entity) {
        Inventory inv = resolveInventory(entity.getLocation(), null, entity);
        openSessions.put(player.getUniqueId(), new StorageSession(player, inv, null, entity, storageType));
        player.openInventory(inv);

        executeOpen(entity.getLocation(), true);
    }

    @Nullable
    public StorageSession remove(UUID uuid) {
        return openSessions.remove(uuid);
    }

    public void clear() {
        Set<Inventory> flushed = new HashSet<>();
        for (StorageSession session : openSessions.values()) {
            if (!flushed.contains(session.inventory())) {
                saveSessionContents(session, false);
                flushed.add(session.inventory());
            }

            session.player().closeInventory();
        }

        openSessions.clear();
    }

    public void saveSessionContents(StorageSession session, boolean playSound) {
        ItemStack[] contents = session.inventory().getContents();

        if (session.isBlock()) {
            StorageInventoryManager.saveToBlock(session.block(), contents, contentsKey, plugin);
        } else if (session.isFurniture()) {
            StorageInventoryManager.saveToEntity(session.entity(), contents, contentsKey);
        } else {
            Log.warn("StorageSessionManager",
                    "Session for {} has neither block nor entity - contents not saved!",
                    session.player().getName());
        }

        executeClose(session.holderLocation(), playSound);
    }

    /**
     * Force-saves and closes all sessions whose holder is within 1.5 blocks of {@code loc}.
     *
     * @param loc          the break location
     * @param preloadCache mutable cache to update with the latest GUI state (may be null)
     */
    public void closeSessionsAt(Location loc, @Nullable Map<BlockCoord, ItemStack[]> preloadCache) {
        Set<Inventory> alreadySaved = new HashSet<>();
        Iterator<Map.Entry<UUID, StorageSession>> it = openSessions.entrySet().iterator();

        while (it.hasNext()) {
            StorageSession session = it.next().getValue();
            Location holderLoc = session.holderLocation();

            boolean sameWorld = holderLoc.getWorld() != null && holderLoc.getWorld().equals(loc.getWorld());
            if (!sameWorld || holderLoc.distanceSquared(loc) > 2.25) continue;

            if (session.type() != StorageType.DISPOSAL && !alreadySaved.contains(session.inventory())) {
                saveSessionContents(session, false);
                alreadySaved.add(session.inventory());

                if (preloadCache != null && session.isBlock() && session.block() != null)
                    preloadCache.put(BlockCoord.of(session.block().getLocation()),
                            session.inventory().getContents());
            }

            it.remove();
            session.player().closeInventory();
        }

        executeClose(loc, true);
    }

    private Inventory resolveInventory(
            Location loc,
            @Nullable Block block,
            @Nullable Entity entity
    ) {
        if (storageType != StorageType.DISPOSAL) {
            Inventory live = findLiveSharedInventory(loc);
            if (live != null) return live;
        }

        StorageInventoryHolder holder = new StorageInventoryHolder(loc);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, title);
        holder.inventory(inv);

        ItemStack[] stored = switch (storageType) {
            case STORAGE, SHULKER -> block != null
                    ? StorageInventoryManager.loadFromBlock(block, contentsKey, plugin)
                    : StorageInventoryManager.loadFromEntity(entity, contentsKey);
            case DISPOSAL -> null;
        };

        StorageInventoryManager.populateInventory(inv, stored);
        return inv;
    }

    @Nullable
    private Inventory findLiveSharedInventory(Location loc) {
        for (StorageSession s : openSessions.values()) {
            Location holderLoc = s.holderLocation();
            if (holderLoc.getWorld() != null
                    && holderLoc.getWorld().equals(loc.getWorld())
                    && holderLoc.distanceSquared(loc) < 0.01)
                return s.inventory();
        }
        return null;
    }

    private void executeOpen(Location location, boolean playSound) {
        if (playSound)
            location.getWorld().playSound(openSound, location.x(), location.y(), location.z());
    }

    private void executeClose(Location location, boolean playSound) {
        if (playSound)
            location.getWorld().playSound(closeSound, location.x(), location.y(), location.z());

    }
}
