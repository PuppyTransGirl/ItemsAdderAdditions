package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.common.utils.BlockCoord;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryManager;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantConfig;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantTransformer;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.session.StorageSessionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable runtime state for one loaded {@code storage} behaviour instance.
 *
 * <p>The behaviour owns lifecycle/config parsing; storage event listeners own Bukkit
 * event flow. This object is the narrow bridge between both sides and keeps the
 * listener classes free from duplicated ids, keys and storage-mode checks.</p>
 */
@NullMarked
public final class StorageRuntime {
    private final JavaPlugin plugin;
    private final String namespacedId;
    private final ItemCategory category;
    private final StorageType storageType;
    private final NamespacedKey contentsKey;
    private final NamespacedKey uniqueIdKey;
    private final StorageSessionManager sessionManager;
    private final ShulkerDropTracker shulkerDropTracker;

    @Nullable
    private final OpenVariantConfig openVariantConfig;

    @Nullable
    private final OpenVariantTransformer openVariantTransformer;

    private final Map<BlockCoord, ItemStack[]> preloadedBlockContents = new HashMap<>();

    public StorageRuntime(
            JavaPlugin plugin,
            String namespacedId,
            ItemCategory category,
            StorageType storageType,
            NamespacedKey contentsKey,
            NamespacedKey uniqueIdKey,
            StorageSessionManager sessionManager,
            ShulkerDropTracker shulkerDropTracker,
            @Nullable OpenVariantConfig openVariantConfig,
            @Nullable OpenVariantTransformer openVariantTransformer
    ) {
        this.plugin = plugin;
        this.namespacedId = namespacedId;
        this.category = category;
        this.storageType = storageType;
        this.contentsKey = contentsKey;
        this.uniqueIdKey = uniqueIdKey;
        this.sessionManager = sessionManager;
        this.shulkerDropTracker = shulkerDropTracker;
        this.openVariantConfig = openVariantConfig;
        this.openVariantTransformer = openVariantTransformer;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public String namespacedId() {
        return namespacedId;
    }

    public ItemCategory category() {
        return category;
    }

    public StorageType storageType() {
        return storageType;
    }

    public NamespacedKey contentsKey() {
        return contentsKey;
    }

    public StorageSessionManager sessionManager() {
        return sessionManager;
    }

    public ShulkerDropTracker shulkerDropTracker() {
        return shulkerDropTracker;
    }

    @Nullable
    public OpenVariantConfig openVariantConfig() {
        return openVariantConfig;
    }

    @Nullable
    public OpenVariantTransformer openVariantTransformer() {
        return openVariantTransformer;
    }

    public Map<BlockCoord, ItemStack[]> preloadedBlockContents() {
        return preloadedBlockContents;
    }

    public boolean matchesClosedBlock(Block block) {
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        return customBlock != null
                && NamespaceUtils.matchesWithRotation(customBlock.getNamespacedID(), namespacedId);
    }

    public boolean matchesClosedId(String id) {
        return NamespaceUtils.matchesWithRotation(id, namespacedId);
    }

    public boolean matchesOpenVariantId(String id) {
        return openVariantConfig != null && id.equals(openVariantConfig.id());
    }

    public boolean hasBlockOpenVariant() {
        return openVariantConfig != null
                && openVariantTransformer != null
                && !openVariantConfig.isFurnitureBased();
    }

    public boolean hasFurnitureOpenVariant() {
        return openVariantConfig != null
                && openVariantTransformer != null
                && openVariantConfig.isFurnitureBased();
    }

    public void preloadBlockContents(Block block, @Nullable ItemStack[] contents) {
        if (contents != null) {
            preloadedBlockContents.put(BlockCoord.of(block.getLocation()), contents);
        }
    }

    @Nullable
    public ItemStack[] consumePreloadedBlockContents(Location location) {
        return preloadedBlockContents.remove(BlockCoord.of(location));
    }

    public void dropContents(Location loc, @Nullable ItemStack @Nullable [] contents) {
        if (contents == null) return;

        for (ItemStack item : contents) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
    }

    public void handleContainerBreak(Location location, @Nullable ItemStack[] contents) {
        Log.debug("StorageBreak", "handleContainerBreak: loc={}, storageType={}, hasContents={}",
                location, storageType, contents != null);

        if (storageType == StorageType.STORAGE) {
            Log.debug("StorageBreak", "STORAGE: dropping contents directly.");
            dropContents(location, contents);
        } else if (storageType == StorageType.SHULKER && contents != null) {
            Log.debug("StorageBreak", "SHULKER: staging drop for injection into shulker item.");
            shulkerDropTracker.stageDrop(location, contents);
        } else {
            Log.debug("StorageBreak", "DISPOSAL or empty SHULKER: no drops handled.");
        }
    }

    public void handleCreativeContainerBreak(Location location, @Nullable ItemStack[] contents) {
        Log.debug("StorageBreak", "handleCreativeContainerBreak: loc={}, storageType={}, hasContents={}",
                location, storageType, contents != null);

        if (storageType == StorageType.STORAGE) {
            Log.debug("StorageBreak", "Creative STORAGE: dropping contents directly.");
            dropContents(location, contents);
        } else if (storageType == StorageType.SHULKER && contents != null) {
            Log.debug("StorageBreak", "Creative SHULKER: staging drop with portable item fallback.");
            shulkerDropTracker.stageCreativeDrop(location, contents);
        } else {
            Log.debug("StorageBreak", "Creative DISPOSAL or empty SHULKER: no drops handled.");
        }
    }

    /**
     * Drops the original closed item after an open-variant holder was broken.
     */
    public void handleOpenVariantBreakDrops(
            Location loc,
            @Nullable ItemStack[] contents
    ) {
        Log.debug("StorageBreak", "handleOpenVariantBreakDrops: loc={}, storageType={}, hasContents={}",
                loc, storageType, contents != null);

        if (storageType == StorageType.DISPOSAL) {
            Log.debug("StorageBreak", "DISPOSAL: skipping open-variant drops.");
            return;
        }

        CustomStack original = CustomStack.getInstance(namespacedId);
        if (original == null) {
            Log.warn(
                    "StorageBehaviour",
                    "Could not find original CustomStack '{}' to drop after open-form break.",
                    namespacedId
            );
            return;
        }

        ItemStack drop = original.getItemStack();

        if (storageType == StorageType.SHULKER && contents != null) {
            Log.debug("StorageBreak", "SHULKER open-variant: injecting contents into drop.");
            StorageInventoryManager.injectIntoItem(drop, contents, contentsKey);
            StorageInventoryManager.stampUniqueId(drop, uniqueIdKey);
        } else if (storageType == StorageType.STORAGE) {
            Log.debug("StorageBreak", "STORAGE open-variant: scattering contents.");
            dropContents(loc, contents);
        }

        Log.debug("StorageBreak", "Dropping original closed item '{}' at {}.", namespacedId, loc);
        loc.getWorld().dropItemNaturally(loc, drop);
    }

    public void handleCreativeOpenVariantBreakDrops(
            Location loc,
            @Nullable ItemStack[] contents
    ) {
        Log.debug("StorageBreak", "handleCreativeOpenVariantBreakDrops: loc={}, storageType={}, hasContents={}",
                loc, storageType, contents != null);

        if (storageType == StorageType.STORAGE) {
            Log.debug("StorageBreak", "Creative STORAGE open-variant: scattering contents.");
            dropContents(loc, contents);
        } else if (storageType == StorageType.SHULKER) {
            Log.debug("StorageBreak", "Creative SHULKER open-variant: dropping original item with contents.");
            shulkerDropTracker.dropPortableItem(loc, contents);
        } else {
            Log.debug("StorageBreak", "Creative DISPOSAL open-variant: skipping drops.");
        }
    }

    /**
     * Tries to read portable-storage contents from either player hand.
     */
    @Nullable
    public ItemStack[] extractFromHand(Player player) {
        ItemStack[] stored = StorageInventoryManager.extractFromItem(
                player.getInventory().getItemInMainHand(),
                contentsKey
        );
        if (stored != null) return stored;

        return StorageInventoryManager.extractFromItem(
                player.getInventory().getItemInOffHand(),
                contentsKey
        );
    }

    public void clear() {
        sessionManager.clear();
        shulkerDropTracker.clear();
        preloadedBlockContents.clear();
        if (openVariantTransformer != null) openVariantTransformer.clear();
    }
}
