package toutouchien.itemsadderadditions.behaviours.executors.storage;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.HashMap;
import java.util.Map;

@NullMarked
public final class OpenVariantTransformer {
    private final OpenVariantConfig config;

    private final Map<BlockCoord, Integer> openCounts = new HashMap<>();
    private final Map<BlockCoord, Entity> liveEntities = new HashMap<>();

    /**
     * Full namespaced ID (including any directional suffix such as {@code _south})
     * of the original block, captured just before it is replaced by the open-variant.
     * Used on close to restore the block with the exact same rotation it had before
     * the player opened it.
     * Only populated when the original holder is a block.
     */
    private final Map<BlockCoord, String> savedBlockIds = new HashMap<>();

    /**
     * Yaw captured from the original furniture entity before it is removed.
     * Stored so it can be re-applied to both the spawned open-variant and the
     * eventually restored original furniture, keeping rotation consistent across
     * the whole open/close cycle.
     * Only populated when the original holder is a furniture (not a block).
     */
    private final Map<BlockCoord, Float> savedYaws = new HashMap<>();

    /**
     * Original ItemStack saved from the furniture's ItemDisplay entity before
     * swapping to the open-variant model.
     * Only populated for {@link OpenVariantConfig.FormType#ITEM_DISPLAY} open-variants.
     * Used to restore the model on close without needing to remove/re-spawn the entity.
     */
    private final Map<BlockCoord, ItemStack> savedItems = new HashMap<>();

    public OpenVariantTransformer(OpenVariantConfig config) {
        this.config = config;
        Log.debug("OpenVariantTransformer", "Created transformer for config: id='{}', type={}",
                config.id(), config.type());
    }

    @Nullable
    public Entity onFirstOpen(
            Location loc,
            boolean isBlock,
            @Nullable Entity originalEntity
    ) {
        BlockCoord key = BlockCoord.of(loc);
        int prev = openCounts.merge(key, 1, Integer::sum) - 1;

        Log.debug("OpenVariantTransformer",
                "onFirstOpen: loc={} isBlock={} prev={} entity={}",
                loc, isBlock, prev,
                originalEntity != null ? originalEntity.getUniqueId() + " (" + originalEntity.getType() + ")" : "null");

        if (prev > 0) {
            Log.debug("OpenVariantTransformer",
                    "onFirstOpen: already transformed (openCount now {}), returning live entity.",
                    prev + 1);
            return liveEntities.get(key);
        }

        return applyTransform(loc, key, isBlock, originalEntity);
    }

    @Nullable
    public Entity onLastClose(Location loc, String originalId, boolean isBlock) {
        BlockCoord key = BlockCoord.of(loc);
        int remaining = openCounts.merge(key, -1, Integer::sum);

        Log.debug("OpenVariantTransformer",
                "onLastClose: loc={} originalId='{}' isBlock={} remaining={}",
                loc, originalId, isBlock, remaining);

        if (remaining > 0) {
            Log.debug("OpenVariantTransformer",
                    "onLastClose: {} session(s) still open, skipping restore.", remaining);
            return null;
        }

        openCounts.remove(key);
        return restoreTransform(loc, key, originalId, isBlock);
    }

    public boolean isTransformed(Location loc) {
        boolean transformed = openCounts.getOrDefault(BlockCoord.of(loc), 0) > 0;
        Log.debug("OpenVariantTransformer", "isTransformed: loc={} → {}", loc, transformed);
        return transformed;
    }

    /**
     * Tries to resolve a rotated form of {@code baseId} by appending {@code suffix}
     * (e.g. {@code "_south"}).  If the suffixed ID corresponds to an existing
     * {@link CustomBlock}, it is returned; otherwise {@code baseId} is returned as-is.
     *
     * <p>This is used when opening a storage block: if the closed variant was
     * {@code "ns:cabinet_closed_south"} and the configured open-variant base ID is
     * {@code "ns:cabinet_open"}, this method tries {@code "ns:cabinet_open_south"}
     * and uses it when it exists.
     *
     * @param baseId the open-variant base namespaced ID from config (no rotation suffix)
     * @param suffix the rotation suffix extracted from the original block (e.g. {@code "_south"},
     *               or {@code ""} for a non-rotated block)
     * @return the best matching namespaced ID to place
     */
    private static String resolveRotatedId(String baseId, String suffix) {
        if (suffix.isEmpty()) return baseId;

        String rotatedId = baseId + suffix;
        if (CustomBlock.getInstance(rotatedId) != null) {
            Log.debug("OpenVariantTransformer",
                    "resolveRotatedId: rotated variant '{}' exists, using it.", rotatedId);
            return rotatedId;
        }

        Log.debug("OpenVariantTransformer",
                "resolveRotatedId: rotated variant '{}' not found, falling back to '{}'.",
                rotatedId, baseId);
        return baseId;
    }

    public void forceRemove(Location loc) {
        BlockCoord key = BlockCoord.of(loc);
        Log.debug("OpenVariantTransformer", "forceRemove: loc={}", loc);

        openCounts.remove(key);
        savedYaws.remove(key);
        savedBlockIds.remove(key);

        ItemStack saved = savedItems.remove(key);
        if (saved != null) {
            Log.debug("OpenVariantTransformer",
                    "forceRemove: discarding saved item (item_display restore skipped - entity is gone).");
        }

        Entity entity = liveEntities.remove(key);
        if (entity != null && entity.isValid()) {
            Log.debug("OpenVariantTransformer",
                    "forceRemove: removing live entity {} ({})", entity.getUniqueId(), entity.getType());
            entity.remove();
        } else {
            Log.debug("OpenVariantTransformer",
                    "forceRemove: no live entity to remove (null or invalid).");
        }
    }

    public void clear() {
        Log.debug("OpenVariantTransformer",
                "clear: removing {} live entity/entities and clearing all state.",
                liveEntities.size());

        liveEntities.values().stream()
                .filter(Entity::isValid)
                .forEach(Entity::remove);

        liveEntities.clear();
        openCounts.clear();
        savedYaws.clear();
        savedItems.clear();
        savedBlockIds.clear();
    }

    /**
     * ITEM_DISPLAY path: the original furniture entity is an {@link ItemDisplay}.
     * We save its current ItemStack and replace it with the open-variant's ItemStack
     * so the model changes without any entity being removed or spawned.
     */
    @Nullable
    private Entity applyItemDisplaySwap(
            Location loc,
            BlockCoord key,
            boolean isBlock,
            @Nullable Entity originalEntity
    ) {
        Log.debug("OpenVariantTransformer",
                "applyItemDisplaySwap: loc={} isBlock={}", loc, isBlock);

        if (isBlock) {
            Log.warn("OpenVariantTransformer",
                    "applyItemDisplaySwap: original holder is a BLOCK but open-variant type is ITEM_DISPLAY. " +
                            "Cannot swap model on a block - open-variant will not be applied.");
            return null;
        }

        if (originalEntity == null || !originalEntity.isValid()) {
            Log.warn("OpenVariantTransformer",
                    "applyItemDisplaySwap: originalEntity is null or invalid at {}. Cannot swap model.", loc);
            return null;
        }

        Log.debug("OpenVariantTransformer",
                "applyItemDisplaySwap: entity={} type={} valid={}",
                originalEntity.getUniqueId(), originalEntity.getType(), originalEntity.isValid());

        if (!(originalEntity instanceof ItemDisplay display)) {
            Log.warn("OpenVariantTransformer",
                    "applyItemDisplaySwap: entity {} is a {} not an ItemDisplay - " +
                            "cannot swap model. Is this really a furniture entity?",
                    originalEntity.getUniqueId(), originalEntity.getType());
            return null;
        }

        // Resolve the open-variant CustomStack to get its ItemStack.
        CustomStack openCs = CustomStack.getInstance(config.id());
        if (openCs == null) {
            Log.warn("OpenVariantTransformer",
                    "applyItemDisplaySwap: CustomStack.getInstance('{}') returned null. " +
                            "Is ItemsAdder fully loaded?", config.id());
            return null;
        }

        ItemStack openItem = openCs.getItemStack();
        Log.debug("OpenVariantTransformer",
                "applyItemDisplaySwap: open-variant CustomStack resolved: id='{}' item={}",
                openCs.getNamespacedID(), openItem);

        // Save the original item so we can restore it on close.
        ItemStack originalItem = display.getItemStack();
        savedItems.put(key, originalItem != null ? originalItem.clone() : new ItemStack(Material.AIR));
        Log.debug("OpenVariantTransformer",
                "applyItemDisplaySwap: saved original item: {}",
                originalItem);

        // Swap the model.
        display.setItemStack(openItem);
        Log.debug("OpenVariantTransformer",
                "applyItemDisplaySwap: model swapped successfully on entity {}.",
                display.getUniqueId());

        // Track the entity so isTransformed / forceRemove can find it if needed.
        liveEntities.put(key, display);
        return display;
    }

    @Nullable
    private Entity applyTransform(
            Location loc,
            BlockCoord key,
            boolean isBlock,
            @Nullable Entity originalEntity
    ) {
        Log.debug("OpenVariantTransformer",
                "applyTransform: loc={} key={} isBlock={} configType={} configId='{}'",
                loc, key, isBlock, config.type(), config.id());

        // When the original holder is a block, capture its full namespaced ID (which may
        // include a directional suffix such as "_south") before we overwrite or clear it.
        // This is used to (a) restore the correct rotation on close, and (b) derive the
        // matching rotated open-variant ID when one exists.
        String rotationSuffix = "";
        if (isBlock) {
            CustomBlock existingBlock = CustomBlock.byAlreadyPlaced(loc.getBlock());
            if (existingBlock != null) {
                String actualId = existingBlock.getNamespacedID();
                savedBlockIds.put(key, actualId);
                String baseId = NamespaceUtils.stripRotationSuffix(actualId);
                rotationSuffix = actualId.substring(baseId.length()); // e.g. "_south" or ""
                Log.debug("OpenVariantTransformer",
                        "applyTransform: captured original block id='{}' rotationSuffix='{}'",
                        actualId, rotationSuffix);
            } else {
                Log.debug("OpenVariantTransformer",
                        "applyTransform: no CustomBlock at loc={} (not a custom block?)", loc);
            }
        }

        // ITEM_DISPLAY open variant
        // The original entity IS already an ItemDisplay (all IA furniture is).
        // We just swap the displayed item in-place - no spawn, no removal.
        if (config.type() == OpenVariantConfig.FormType.ITEM_DISPLAY) {
            return applyItemDisplaySwap(loc, key, isBlock, originalEntity);
        }

        // FURNITURE open variant
        if (config.isFurnitureBased()) {
            float yaw = captureAndClearHolder(loc, key, isBlock, originalEntity);

            Block supportBlock = furnitureSupportBlock(loc, isBlock);
            Log.debug("OpenVariantTransformer",
                    "applyTransform[FURNITURE]: spawning '{}' at support block {} (isBlock={})",
                    config.id(), supportBlock.getLocation(), isBlock);

            CustomFurniture spawned = CustomFurniture.spawn(config.id(), supportBlock);

            if (spawned == null || spawned.getEntity() == null) {
                Log.warn("OpenVariantTransformer",
                        "applyTransform[FURNITURE]: CustomFurniture.spawn returned null for '{}' at {}.",
                        config.id(), loc);
                return null;
            }

            Entity entity = spawned.getEntity();
            Log.debug("OpenVariantTransformer",
                    "applyTransform[FURNITURE]: spawned entity {} ({}) at {}",
                    entity.getUniqueId(), entity.getType(), entity.getLocation());

            if (!isBlock) {
                entity.setRotation(yaw, 0f);
                Log.debug("OpenVariantTransformer",
                        "applyTransform[FURNITURE]: applied yaw={} to spawned entity.", yaw);
            }

            liveEntities.put(key, entity);
            return entity;
        }

        // BLOCK open variant
        captureAndClearHolder(loc, key, isBlock, originalEntity);

        // If the original block had a directional rotation suffix, try to place the same
        // rotated form of the open variant (e.g. "ns:cabinet_open_south"). Fall back to
        // the base open-variant ID if no such rotated variant exists in ItemsAdder.
        String openBlockId = resolveRotatedId(config.id(), rotationSuffix);
        Log.debug("OpenVariantTransformer",
                "applyTransform[BLOCK]: placing block '{}' at {}", openBlockId, loc);

        if (!placeBlock(openBlockId, loc)) {
            Log.warn("OpenVariantTransformer",
                    "applyTransform[BLOCK]: failed to place open-form block '{}' at {}.",
                    openBlockId, loc);
        } else {
            Log.debug("OpenVariantTransformer",
                    "applyTransform[BLOCK]: block placed successfully.");
        }

        return null;
    }

    /**
     * ITEM_DISPLAY restore: swap the ItemStack back to the original model.
     * The entity was never removed so it's still valid.
     */
    @Nullable
    private Entity restoreItemDisplaySwap(Location loc, BlockCoord key) {
        Log.debug("OpenVariantTransformer",
                "restoreItemDisplaySwap: loc={}", loc);

        Entity entity = liveEntities.remove(key);
        ItemStack savedItem = savedItems.remove(key);

        Log.debug("OpenVariantTransformer",
                "restoreItemDisplaySwap: live entity={} savedItem={}",
                entity != null ? entity.getUniqueId() + " valid=" + entity.isValid() : "null",
                savedItem);

        if (entity == null) {
            Log.warn("OpenVariantTransformer",
                    "restoreItemDisplaySwap: no live entity found for loc={}. Cannot restore model.", loc);
            return null;
        }

        if (!entity.isValid()) {
            Log.warn("OpenVariantTransformer",
                    "restoreItemDisplaySwap: entity {} is no longer valid. Cannot restore model.",
                    entity.getUniqueId());
            return null;
        }

        if (!(entity instanceof ItemDisplay display)) {
            Log.warn("OpenVariantTransformer",
                    "restoreItemDisplaySwap: live entity {} is a {}, not ItemDisplay. Cannot restore model.",
                    entity.getUniqueId(), entity.getType());
            return null;
        }

        if (savedItem == null) {
            Log.warn("OpenVariantTransformer",
                    "restoreItemDisplaySwap: no saved item found for loc={}. Model not restored.", loc);
            return entity;
        }

        display.setItemStack(savedItem);
        Log.debug("OpenVariantTransformer",
                "restoreItemDisplaySwap: model restored successfully to {} on entity {}.",
                savedItem, display.getUniqueId());

        return display;
    }

    /**
     * Removes whatever is currently occupying the storage location so the open-variant
     * can be placed there, and captures the original furniture's yaw for later re-use.
     *
     * <ul>
     *   <li>Block holder  → Block open-variant      : nothing to do (placeBlock overwrites); yaw = 0</li>
     *   <li>Block holder  → Furniture open-variant   : set block to AIR; yaw = 0</li>
     *   <li>Furniture holder → any open-variant      : remove furniture entity; yaw captured</li>
     * </ul>
     *
     * @return the yaw of the removed furniture entity, or {@code 0f} when the original is a block
     */
    private float captureAndClearHolder(
            Location loc,
            BlockCoord key,
            boolean isBlock,
            @Nullable Entity originalEntity
    ) {
        Log.debug("OpenVariantTransformer",
                "captureAndClearHolder: loc={} isBlock={} entity={}",
                loc, isBlock,
                originalEntity != null ? originalEntity.getUniqueId() : "null");

        if (isBlock) {
            if (config.isFurnitureBased()) {
                Log.debug("OpenVariantTransformer",
                        "captureAndClearHolder: BLOCK→FURNITURE - setting block to AIR at {}.", loc);
                loc.getBlock().setType(Material.AIR);
            }
            return 0f;
        }

        float yaw = 0f;
        if (originalEntity != null && originalEntity.isValid()) {
            yaw = originalEntity.getLocation().getYaw();
            savedYaws.put(key, yaw);
            Log.debug("OpenVariantTransformer",
                    "captureAndClearHolder: captured yaw={} from entity {}.", yaw, originalEntity.getUniqueId());

            CustomFurniture furniture = CustomFurniture.byAlreadySpawned(originalEntity);
            if (furniture != null) {
                Log.debug("OpenVariantTransformer",
                        "captureAndClearHolder: calling CustomFurniture.remove() on entity {}.",
                        originalEntity.getUniqueId());
                furniture.remove(false);
            } else {
                Log.warn("OpenVariantTransformer",
                        "captureAndClearHolder: CustomFurniture.byAlreadySpawned returned null for " +
                                "entity {} - falling back to raw entity.remove().",
                        originalEntity.getUniqueId());
                originalEntity.remove();
            }
        } else {
            Log.warn("OpenVariantTransformer",
                    "captureAndClearHolder: originalEntity is null or invalid at {} - " +
                            "open-variant may overlap the original.", loc);
        }

        return yaw;
    }

    /**
     * Removes the currently displayed open-variant so the original can be restored.
     * For furniture open-variants the live entity is looked up from {@link #liveEntities}.
     * For block open-variants the block at {@code loc} is cleared.
     */
    private void removeOpenVariant(Location loc, BlockCoord key) {
        Log.debug("OpenVariantTransformer",
                "removeOpenVariant: loc={} configType={}", loc, config.type());

        if (config.isFurnitureBased()) {
            Entity entity = liveEntities.remove(key);
            if (entity != null && entity.isValid()) {
                CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entity);
                if (furniture != null) {
                    Log.debug("OpenVariantTransformer",
                            "removeOpenVariant: calling CustomFurniture.remove() on entity {}.",
                            entity.getUniqueId());
                    furniture.remove(false);
                } else {
                    Log.warn("OpenVariantTransformer",
                            "removeOpenVariant: CustomFurniture.byAlreadySpawned returned null for " +
                                    "entity {} - falling back to raw entity.remove().",
                            entity.getUniqueId());
                    entity.remove();
                }
            } else {
                Log.debug("OpenVariantTransformer",
                        "removeOpenVariant: no live FURNITURE entity found (null or invalid).");
            }
        } else {
            Log.debug("OpenVariantTransformer",
                    "removeOpenVariant: BLOCK open-variant - setting block to AIR at {}.", loc);
            loc.getBlock().setType(Material.AIR);
        }
    }

    /**
     * Returns the block that ItemsAdder should use as the "support" when spawning furniture.
     *
     * <p>When the original holder was a block, the furniture must be spawned one block
     * below the storage location (so it ends up visually at the right height).
     * When the original holder was already a furniture (entity), the location itself
     * is the support block.
     */
    private Block furnitureSupportBlock(Location loc, boolean replacingBlockHolder) {
        Block support = replacingBlockHolder
                ? loc.clone().subtract(0, 1, 0).getBlock()
                : loc.getBlock();
        Log.debug("OpenVariantTransformer",
                "furnitureSupportBlock: loc={} replacingBlockHolder={} → support={}",
                loc, replacingBlockHolder, support.getLocation());
        return support;
    }

    @Nullable
    private Entity restoreTransform(
            Location loc,
            BlockCoord key,
            String originalId,
            boolean originalIsBlock
    ) {
        Log.debug("OpenVariantTransformer",
                "restoreTransform: loc={} originalId='{}' originalIsBlock={} configType={}",
                loc, originalId, originalIsBlock, config.type());

        // ITEM_DISPLAY restore - swap the model back
        if (config.type() == OpenVariantConfig.FormType.ITEM_DISPLAY) {
            return restoreItemDisplaySwap(loc, key);
        }

        // Remove the open-variant that is currently displayed.
        removeOpenVariant(loc, key);

        if (originalIsBlock) {
            // Use the actual ID that was captured on open (includes any rotation suffix such as
            // "_south"). Fall back to originalId if nothing was captured (shouldn't happen).
            String restoreId = savedBlockIds.remove(key);
            if (restoreId == null) {
                Log.warn("OpenVariantTransformer",
                        "restoreTransform[BLOCK]: no saved block id for key={}, " +
                                "falling back to base id '{}'.", key, originalId);
                restoreId = originalId;
            }
            savedYaws.remove(key);
            Log.debug("OpenVariantTransformer",
                    "restoreTransform[BLOCK]: placing original block '{}'.", restoreId);
            if (!placeBlock(restoreId, loc)) {
                Log.warn("OpenVariantTransformer",
                        "restoreTransform[BLOCK]: failed to restore original block '{}' at {}.",
                        restoreId, loc);
            } else {
                Log.debug("OpenVariantTransformer",
                        "restoreTransform[BLOCK]: block restored successfully.");
            }
            return null;
        }

        Float savedYaw = savedYaws.remove(key);
        Log.debug("OpenVariantTransformer",
                "restoreTransform[FURNITURE]: spawning original furniture '{}', savedYaw={}",
                originalId, savedYaw);

        Block supportBlock = furnitureSupportBlock(loc, false);
        CustomFurniture restored = CustomFurniture.spawn(originalId, supportBlock);

        if (restored == null || restored.getEntity() == null) {
            Log.warn("OpenVariantTransformer",
                    "restoreTransform[FURNITURE]: CustomFurniture.spawn returned null for '{}' at {}.",
                    originalId, loc);
            return null;
        }

        Entity restoredEntity = restored.getEntity();
        Log.debug("OpenVariantTransformer",
                "restoreTransform[FURNITURE]: spawned restored entity {} ({}) at {}",
                restoredEntity.getUniqueId(), restoredEntity.getType(), restoredEntity.getLocation());

        if (savedYaw != null) {
            restoredEntity.setRotation(savedYaw, 0f);
            Log.debug("OpenVariantTransformer",
                    "restoreTransform[FURNITURE]: applied savedYaw={} to restored entity.", savedYaw);
        }

        return restoredEntity;
    }

    private boolean placeBlock(String namespacedId, Location loc) {
        Log.debug("OpenVariantTransformer",
                "placeBlock: namespacedId='{}' loc={}", namespacedId, loc);
        CustomBlock cb = CustomBlock.getInstance(namespacedId);
        if (cb == null) {
            Log.warn("OpenVariantTransformer",
                    "placeBlock: CustomBlock.getInstance('{}') returned null.", namespacedId);
            return false;
        }

        cb.place(loc);
        Log.debug("OpenVariantTransformer", "placeBlock: placed '{}' at {}.", namespacedId, loc);
        return true;
    }
}
