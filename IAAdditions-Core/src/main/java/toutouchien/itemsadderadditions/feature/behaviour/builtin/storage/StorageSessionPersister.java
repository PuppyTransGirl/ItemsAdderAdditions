package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@NullMarked
final class StorageSessionPersister {
    private static final String LOG_TAG = "StorageSession";

    private final JavaPlugin plugin;
    private final StorageType storageType;
    private final NamespacedKey contentsKey;
    private final String originalNamespacedId;
    @Nullable private final OpenVariantTransformer openVariantTransformer;

    StorageSessionPersister(
            JavaPlugin plugin,
            StorageType storageType,
            NamespacedKey contentsKey,
            String originalNamespacedId,
            @Nullable OpenVariantTransformer openVariantTransformer
    ) {
        this.plugin = plugin;
        this.storageType = storageType;
        this.contentsKey = contentsKey;
        this.originalNamespacedId = originalNamespacedId;
        this.openVariantTransformer = openVariantTransformer;
    }

    void saveAfterClose(StorageSession session, boolean lastAtLocation) {
        if (storageType == StorageType.DISPOSAL) {
            return;
        }

        ItemStack[] contents = session.inventory().getContents();
        Location location = session.holderLocation();

        if (session.isBlock()) {
            StorageInventoryManager.saveToBlock(session.block(), contents, contentsKey, plugin);
            if (lastAtLocation && openVariantTransformer != null) {
                openVariantTransformer.onLastClose(location, originalNamespacedId, true);
            }
            return;
        }

        if (session.isFurniture()) {
            if (lastAtLocation) {
                Entity saveTarget = restoredFurnitureTarget(location, session.entity());
                StorageInventoryManager.saveToEntity(saveTarget, contents, contentsKey);
            }
            return;
        }

        Log.warn(LOG_TAG, "Session for {} has neither block nor entity - contents not saved.",
                session.player().getName());
    }

    Set<Inventory> saveBeforeHolderBreak(
            Iterable<StorageSession> sessions,
            @Nullable Map<BlockCoord, ItemStack[]> preloadCache
    ) {
        Set<Inventory> savedInventories = new HashSet<>();
        for (StorageSession session : sessions) {
            if (storageType == StorageType.DISPOSAL || savedInventories.contains(session.inventory())) {
                continue;
            }

            ItemStack[] contents = session.inventory().getContents();
            if (session.isBlock()) {
                StorageInventoryManager.saveToBlock(session.block(), contents, contentsKey, plugin);
                if (preloadCache != null) {
                    preloadCache.put(BlockCoord.of(session.block().getLocation()), contents);
                }
            } else if (session.isFurniture() && openVariantTransformer == null) {
                StorageInventoryManager.saveToEntity(session.entity(), contents, contentsKey);
            }

            savedInventories.add(session.inventory());
        }
        return savedInventories;
    }

    void forceRemoveOpenVariant(Location location) {
        if (openVariantTransformer != null) {
            openVariantTransformer.forceRemove(location);
        }
    }

    private Entity restoredFurnitureTarget(Location location, Entity fallback) {
        if (openVariantTransformer == null) {
            return fallback;
        }

        Entity restored = openVariantTransformer.onLastClose(location, originalNamespacedId, false);
        return restored != null ? restored : fallback;
    }
}
