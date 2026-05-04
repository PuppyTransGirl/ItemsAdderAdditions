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
 *
 * <p>When an {@link OpenVariantTransformer} is provided, the session manager
 * coordinates visual open/close swaps around inventory open and close events:
 * <ul>
 *   <li>On the <em>first</em> open at a location the transformer swaps the
 *       block/furniture to the configured open-form variant.</li>
 *   <li>On the <em>last</em> close the transformer restores the original.
 *       For furniture-backed storage the restored entity is used as the
 *       persistence target instead of the now-stale session entity.</li>
 * </ul>
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

    /**
     * namespacedID of the original storage item; used by the transformer for restoration.
     */
    private final String originalNamespacedId;

    /**
     * {@code null} when no {@code open_variant} is configured.
     */
    @Nullable private final OpenVariantTransformer openVariantTransformer;

    public StorageSessionManager(
            int rows,
            Component title,
            StorageType storageType,
            NamespacedKey contentsKey,
            JavaPlugin plugin,
            @Nullable Sound openSound,
            @Nullable Sound closeSound,
            String originalNamespacedId,
            @Nullable OpenVariantTransformer openVariantTransformer
    ) {
        this.rows = rows;
        this.title = title;
        this.storageType = storageType;
        this.contentsKey = contentsKey;
        this.plugin = plugin;
        this.openSound = openSound;
        this.closeSound = closeSound;
        this.originalNamespacedId = originalNamespacedId;
        this.openVariantTransformer = openVariantTransformer;
    }

    public void openForBlock(Player player, Block block) {
        Location loc = block.getLocation();
        boolean isFirstAtLocation = !hasOpenSessionsAt(loc);

        Inventory inv = resolveInventory(loc, block, null);
        openSessions.put(player.getUniqueId(),
                new StorageSession(player, inv, block, null, storageType));
        player.openInventory(inv);
        CoreProtectUtils.logInteraction(player.getName(), loc);

        if (isFirstAtLocation && openVariantTransformer != null) {
            openVariantTransformer.onFirstOpen(loc, true, null);
        }

        executeOpen(loc, true);
    }

    public void openForEntity(Player player, Entity entity) {
        Location loc = entity.getLocation();
        boolean isFirstAtLocation = !hasOpenSessionsAt(loc);

        // Load inventory contents BEFORE the entity is potentially removed by the transformer.
        Inventory inv = resolveInventory(loc, null, entity);
        openSessions.put(player.getUniqueId(),
                new StorageSession(player, inv, null, entity, storageType));
        player.openInventory(inv);

        if (isFirstAtLocation && openVariantTransformer != null) {
            // Pass the entity so the transformer can remove it when swapping to the open variant.
            openVariantTransformer.onFirstOpen(loc, false, entity);
        }

        executeOpen(loc, true);
    }

    @Nullable
    public StorageSession remove(UUID uuid) {
        return openSessions.remove(uuid);
    }

    /**
     * Saves the inventory contents of {@code session} to its backing storage and
     * (if this is the last session at that location) restores the open-form visual.
     *
     * <h3>Furniture + open-form save order</h3>
     * When an {@link OpenVariantTransformer} is active and the original holder is
     * furniture, the original entity was removed during {@link #openForEntity}.
     * {@link OpenVariantTransformer#onLastClose} spawns it back and returns it;
     * that fresh entity is used as the save target.  For non-last closes the
     * inventory is still live in memory (shared by other open sessions), so no
     * PDC write is needed until the final close.
     *
     * @param session   the session to persist
     * @param playSound whether to play the close sound (suppressed for force-closes)
     */
    public void saveSessionContents(StorageSession session, boolean playSound) {
        ItemStack[] contents = session.inventory().getContents();
        Location holderLoc = session.holderLocation();
        boolean isLastAtLocation = !hasOpenSessionsAt(holderLoc);

        if (session.isBlock()) {
            StorageInventoryManager.saveToBlock(session.block(), contents, contentsKey, plugin);

            if (isLastAtLocation && openVariantTransformer != null) {
                openVariantTransformer.onLastClose(holderLoc, originalNamespacedId, true);
            }
        } else if (session.isFurniture()) {
            if (isLastAtLocation) {
                if (openVariantTransformer != null) {
                    // Restore original furniture and use the returned entity as save target.
                    Entity saveTarget = openVariantTransformer.onLastClose(
                            holderLoc, originalNamespacedId, false);
                    // If restoration failed (returns null), fall back to the session entity.
                    // The session entity is stale (removed) when open_variant is active, so this
                    // is best-effort; the caller should verify transformer logs on failure.
                    StorageInventoryManager.saveToEntity(
                            saveTarget != null ? saveTarget : session.entity(),
                            contents, contentsKey);
                } else {
                    StorageInventoryManager.saveToEntity(session.entity(), contents, contentsKey);
                }
            }
            // else: other sessions at this location are still open and share the same Bukkit
            // Inventory instance - the data is live in memory; defer the PDC write.
        } else {
            Log.warn("StorageSessionManager",
                    "Session for {} has neither block nor entity - contents not saved!",
                    session.player().getName());
        }

        if (isLastAtLocation) {
            executeClose(holderLoc, playSound);
        }
    }

    /**
     * Force-saves and closes all sessions whose holder is within 1.5 blocks of {@code loc}.
     * Called from break-event handlers when the storage holder is destroyed.
     *
     * <p>If an {@link OpenVariantTransformer} is active, its state is forcibly cleared
     * (without visual restoration, since the holder no longer exists).
     *
     * @param loc          the break location
     * @param preloadCache mutable cache to update with the latest GUI state; may be {@code null}
     */
    public void closeSessionsAt(Location loc, @Nullable Map<BlockCoord, ItemStack[]> preloadCache) {
        Set<Inventory> alreadySaved = new HashSet<>();
        Iterator<Map.Entry<UUID, StorageSession>> it = openSessions.entrySet().iterator();

        while (it.hasNext()) {
            StorageSession session = it.next().getValue();
            Location holderLoc = session.holderLocation();

            boolean sameWorld = holderLoc.getWorld() != null
                    && holderLoc.getWorld().equals(loc.getWorld());
            if (!sameWorld || holderLoc.distanceSquared(loc) > 2.25) continue;

            if (session.type() != StorageType.DISPOSAL
                    && !alreadySaved.contains(session.inventory())) {
                ItemStack[] contents = session.inventory().getContents();

                if (session.isBlock()) {
                    StorageInventoryManager.saveToBlock(session.block(), contents, contentsKey, plugin);
                } else if (session.isFurniture()) {
                    // The storage is being destroyed; no entity to restore to.
                    // Contents will be dropped by the caller (StorageBehaviour).
                    // For furniture + open_variant the session entity was already removed
                    // on open; we skip the PDC write since the entity is gone.
                    if (openVariantTransformer == null) {
                        StorageInventoryManager.saveToEntity(session.entity(), contents, contentsKey);
                    }
                }

                alreadySaved.add(session.inventory());

                if (preloadCache != null && session.isBlock() && session.block() != null) {
                    preloadCache.put(
                            BlockCoord.of(session.block().getLocation()),
                            contents);
                }
            }

            it.remove();
            session.player().closeInventory();
        }

        if (!alreadySaved.isEmpty()) {
            // Storage holder is gone - forcibly clean up transformer state without restoring.
            if (openVariantTransformer != null) {
                openVariantTransformer.forceRemove(loc);
            }
            executeClose(loc, true);
        }
    }

    /**
     * Saves and closes every open session (called on behaviour unload).
     */
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

    public void executeClose(Location location, boolean playSound) {
        if (playSound && closeSound != null)
            location.getWorld().playSound(closeSound, location.x(), location.y(), location.z());
    }

    private void executeOpen(Location location, boolean playSound) {
        if (playSound && openSound != null)
            location.getWorld().playSound(openSound, location.x(), location.y(), location.z());
    }

    /**
     * Returns {@code true} if at least one session is currently open at {@code loc}
     * (within 0.1 blocks, to account for floating-point noise in entity locations).
     */
    private boolean hasOpenSessionsAt(Location loc) {
        for (StorageSession s : openSessions.values()) {
            Location holderLoc = s.holderLocation();
            if (holderLoc.getWorld() != null
                    && holderLoc.getWorld().equals(loc.getWorld())
                    && holderLoc.distanceSquared(loc) < 0.01)
                return true;
        }
        return false;
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

    /**
     * Returns the live inventory contents at {@code loc} without closing any sessions.
     * Used when the open-form is broken so we can drop the real contents.
     */
    @Nullable
    public ItemStack[] getLiveContentsAt(Location loc) {
        Inventory live = findLiveSharedInventory(loc);
        return live != null ? live.getContents() : null;
    }

    /**
     * Opens the GUI for {@code player} at a location that is <em>already</em> transformed.
     * The transformer is not called again; the shared inventory is reused directly.
     *
     * <p>Used when a player interacts with the open-form block/furniture while at least
     * one session is already open there.
     */
    public void openForPlayerAtTransformedLocation(Player player, Location loc,
                                                   @Nullable Block block, @Nullable Entity entity) {
        Inventory inv = resolveInventory(loc, block, entity); // hits the shared-inventory fast path
        StorageSession session = new StorageSession(player, inv, block, entity, storageType);
        openSessions.put(player.getUniqueId(), session);
        player.openInventory(inv);
        CoreProtectUtils.logInteraction(player.getName(), loc);
        executeOpen(loc, true);
    }
}
