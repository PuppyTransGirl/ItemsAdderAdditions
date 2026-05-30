package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.session;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
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
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.utils.BlockCoord;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageType;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryResolver;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventorySpec;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantTransformer;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.sound.StorageSoundPlayer;
import toutouchien.itemsadderadditions.integration.hook.CoreProtectHook;

import java.util.*;

/**
 * Coordinates open storage GUI sessions for one loaded storage behaviour.
 *
 * <p>This class now delegates storage persistence, inventory creation, session
 * indexing, sounds, and open-variant restoration to dedicated collaborators. Its
 * public API is intentionally event-oriented: listeners ask it to open, close,
 * destroy, or inspect sessions.</p>
 */
@NullMarked
public final class StorageSessionManager {
    private static final double BREAK_MATCH_DISTANCE_SQUARED = 2.25;

    private final StorageType storageType;
    private final StorageSessionRegistry sessions = new StorageSessionRegistry();
    private final StorageInventoryResolver inventories;
    private final StorageSessionPersister persister;
    private final StorageSoundPlayer sounds;
    @Nullable private final OpenVariantTransformer openVariantTransformer;

    public StorageSessionManager(
            int rows,
            @Nullable StorageInventorySpec spec,
            Component title,
            StorageType storageType,
            NamespacedKey contentsKey,
            JavaPlugin plugin,
            @Nullable Sound openSound,
            @Nullable Sound closeSound,
            String originalNamespacedId,
            @Nullable OpenVariantTransformer openVariantTransformer
    ) {
        this.storageType = storageType;
        this.openVariantTransformer = openVariantTransformer;
        this.inventories = new StorageInventoryResolver(sessions, rows, spec, title, storageType, contentsKey, plugin);
        this.persister = new StorageSessionPersister(
                plugin, storageType, contentsKey, originalNamespacedId, openVariantTransformer);
        this.sounds = new StorageSoundPlayer(openSound, closeSound);
    }

    public void openForBlock(Player player, Block block) {
        Location location = block.getLocation();
        boolean firstAtLocation = !sessions.hasAt(location);
        Inventory inventory = inventories.openFor(player, location, block, null);

        open(player, new StorageSession(player, inventory, block, null, storageType), location);

        if (firstAtLocation && openVariantTransformer != null) {
            openVariantTransformer.onFirstOpen(location, true, null);
        }
    }

    public void openForEntity(Player player, Entity entity) {
        Location location = entity.getLocation();
        boolean firstAtLocation = !sessions.hasAt(location);

        // Load contents and open before the transformer can remove/replace the entity.
        Inventory inventory = inventories.openFor(player, location, null, entity);
        open(player, new StorageSession(player, inventory, null, entity, storageType), location);

        if (firstAtLocation && openVariantTransformer != null) {
            openVariantTransformer.onFirstOpen(location, false, entity);
        }
    }

    public void openForPlayerAtTransformedLocation(
            Player player,
            Location location,
            @Nullable Block block,
            @Nullable Entity entity
    ) {
        Inventory inventory = inventories.openFor(player, location, block, entity);
        open(player, new StorageSession(player, inventory, block, entity, storageType), location);
    }

    @Nullable
    public StorageSession remove(UUID uuid) {
        return sessions.remove(uuid);
    }

    public void saveSessionContents(StorageSession session, boolean playSound) {
        Location location = session.holderLocation();
        boolean lastAtLocation = !sessions.hasAt(location);

        persister.saveAfterClose(session, lastAtLocation);
        if (lastAtLocation) {
            sounds.playClose(location, playSound);
        }
    }

    public void closeSessionsAt(Location location, @Nullable Map<BlockCoord, ItemStack[]> preloadCache) {
        var matchingSessions = sessions.near(location, BREAK_MATCH_DISTANCE_SQUARED);
        Log.debug("StorageSession", "closeSessionsAt {}: matchingSessions={}, hasPreloadCache={}",
                location, matchingSessions.size(), preloadCache != null);

        Set<Inventory> savedInventories = persister.saveBeforeHolderBreak(matchingSessions, preloadCache);
        Log.debug("StorageSession", "closeSessionsAt {}: savedInventories={}", location, savedInventories.size());

        for (StorageSession session : matchingSessions) {
            sessions.remove(session.player().getUniqueId());
            session.player().closeInventory();
        }

        if (!savedInventories.isEmpty()) {
            Log.debug("StorageSession", "closeSessionsAt {}: calling forceRemoveOpenVariant.", location);
            persister.forceRemoveOpenVariant(location);
            sounds.playClose(location, true);
        }
    }

    /**
     * Closes any open sessions at {@code location} due to the open-variant furniture entity
     * itself being broken by IA. Unlike {@link #closeSessionsAt}, this does NOT attempt to
     * remove the open-variant entity - IA already owns that removal via FurnitureBreakEvent.
     */
    public void closeSessionsForOpenVariantBreak(Location location) {
        var matchingSessions = sessions.near(location, BREAK_MATCH_DISTANCE_SQUARED);
        Log.debug("StorageSession", "closeSessionsForOpenVariantBreak {}: matchingSessions={}",
                location, matchingSessions.size());

        Set<Inventory> savedInventories = persister.saveBeforeHolderBreak(matchingSessions, null);
        Log.debug("StorageSession", "closeSessionsForOpenVariantBreak {}: savedInventories={}",
                location, savedInventories.size());

        for (StorageSession session : matchingSessions) {
            sessions.remove(session.player().getUniqueId());
            session.player().closeInventory();
        }

        if (!savedInventories.isEmpty()) {
            sounds.playClose(location, true);
        }
    }

    public void clear() {
        Collection<StorageSession> snapshot = sessions.all();
        sessions.clear();
        Set<Inventory> flushed = new HashSet<>();
        for (StorageSession session : snapshot) {
            if (flushed.add(session.inventory())) {
                saveSessionContents(session, false);
            }
            session.player().closeInventory();
        }
    }

    public void executeClose(Location location, boolean playSound) {
        sounds.playClose(location, playSound);
    }

    @Nullable
    public ItemStack[] getLiveContentsAt(Location location) {
        return inventories.liveContentsAt(location);
    }

    private void open(Player player, StorageSession session, Location location) {
        sessions.add(session);
        CoreProtectHook.INSTANCE.logInteraction(player.getName(), location);
        sounds.playOpen(location);
    }
}
