package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

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
import toutouchien.itemsadderadditions.integration.hook.CoreProtectUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
        this.inventories = new StorageInventoryResolver(sessions, rows, title, storageType, contentsKey, plugin);
        this.persister = new StorageSessionPersister(
                plugin, storageType, contentsKey, originalNamespacedId, openVariantTransformer);
        this.sounds = new StorageSoundPlayer(openSound, closeSound);
    }

    public void openForBlock(Player player, Block block) {
        Location location = block.getLocation();
        boolean firstAtLocation = !sessions.hasAt(location);
        Inventory inventory = inventories.resolve(location, block, null);

        open(player, new StorageSession(player, inventory, block, null, storageType), location);

        if (firstAtLocation && openVariantTransformer != null) {
            openVariantTransformer.onFirstOpen(location, true, null);
        }
    }

    public void openForEntity(Player player, Entity entity) {
        Location location = entity.getLocation();
        boolean firstAtLocation = !sessions.hasAt(location);

        // Load contents before the transformer can remove/replace the entity.
        Inventory inventory = inventories.resolve(location, null, entity);
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
        Inventory inventory = inventories.resolve(location, block, entity);
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
        Set<Inventory> savedInventories = persister.saveBeforeHolderBreak(matchingSessions, preloadCache);

        for (StorageSession session : matchingSessions) {
            sessions.remove(session.player().getUniqueId());
            session.player().closeInventory();
        }

        if (!savedInventories.isEmpty()) {
            persister.forceRemoveOpenVariant(location);
            sounds.playClose(location, true);
        }
    }

    public void clear() {
        Set<Inventory> flushed = new HashSet<>();
        for (StorageSession session : sessions.all()) {
            if (flushed.add(session.inventory())) {
                saveSessionContents(session, false);
            }
            session.player().closeInventory();
        }
        sessions.clear();
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
        player.openInventory(session.inventory());
        CoreProtectUtils.logInteraction(player.getName(), location);
        sounds.playOpen(location);
    }
}
