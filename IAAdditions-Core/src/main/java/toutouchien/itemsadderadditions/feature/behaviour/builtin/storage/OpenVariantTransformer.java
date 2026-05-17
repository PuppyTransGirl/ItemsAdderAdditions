package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

/**
 * Handles visual closed/open swaps for one storage item.
 *
 * <p>The transformer is reference-counted by location: the first opener applies
 * the visual open form, and the final closer restores the original holder. The
 * actual storage data is persisted by {@link StorageSessionPersister}; this class
 * only owns block/furniture/model transformations.</p>
 */
@NullMarked
public final class OpenVariantTransformer {
    private static final String LOG_TAG = "OpenVariant";

    private final OpenVariantConfig config;
    private final OpenVariantState state = new OpenVariantState();

    public OpenVariantTransformer(OpenVariantConfig config) {
        this.config = config;
    }

    @Nullable
    public Entity onFirstOpen(Location location, boolean originalIsBlock, @Nullable Entity originalEntity) {
        BlockCoord key = BlockCoord.of(location);
        int openers = state.increment(key);
        if (openers > 1) {
            return state.liveEntity(key);
        }

        return applyOpenVariant(location, key, originalIsBlock, originalEntity);
    }

    @Nullable
    public Entity onLastClose(Location location, String originalId, boolean originalIsBlock) {
        BlockCoord key = BlockCoord.of(location);
        int remaining = state.decrement(key);
        if (remaining > 0) {
            return null;
        }

        return restoreClosedVariant(location, key, originalId, originalIsBlock);
    }

    public boolean isTransformed(Location location) {
        return state.isOpen(BlockCoord.of(location));
    }

    public void forceRemove(Location location) {
        BlockCoord key = BlockCoord.of(location);
        state.forget(key);

        Entity liveEntity = state.removeLiveEntity(key);
        if (liveEntity != null && liveEntity.isValid()) {
            liveEntity.remove();
        }
    }

    public void clear() {
        state.clear();
    }

    @Nullable
    private Entity applyOpenVariant(
            Location location,
            BlockCoord key,
            boolean originalIsBlock,
            @Nullable Entity originalEntity
    ) {
        String rotationSuffix = captureOriginalBlockState(location, key, originalIsBlock);

        return switch (config.type()) {
            case ITEM_DISPLAY -> swapItemDisplayModel(key, originalIsBlock, originalEntity);
            case FURNITURE -> spawnOpenFurniture(location, key, originalIsBlock, originalEntity);
            case BLOCK -> placeOpenBlock(location, key, originalIsBlock, originalEntity, rotationSuffix);
        };
    }

    @Nullable
    private Entity restoreClosedVariant(
            Location location,
            BlockCoord key,
            String originalId,
            boolean originalIsBlock
    ) {
        if (config.type() == OpenVariantConfig.FormType.ITEM_DISPLAY) {
            return restoreItemDisplayModel(key);
        }

        removeOpenVariant(location, key);

        if (originalIsBlock) {
            String restoreId = state.removeSavedBlockId(key);
            if (restoreId == null) {
                restoreId = originalId;
            }
            OpenVariantPlacement.placeBlock(restoreId, location);
            state.removeSavedYaw(key);
            return null;
        }

        Float savedYaw = state.removeSavedYaw(key);
        return OpenVariantPlacement.spawnFurniture(originalId, location, false, savedYaw);
    }

    private String captureOriginalBlockState(Location location, BlockCoord key, boolean originalIsBlock) {
        if (!originalIsBlock) {
            return "";
        }

        OpenVariantPlacement.BlockState blockState = OpenVariantPlacement.captureBlockState(location);
        if (blockState.id() != null) {
            state.savedBlockId(key, blockState.id());
        }
        return blockState.rotationSuffix();
    }

    @Nullable
    private Entity swapItemDisplayModel(BlockCoord key, boolean originalIsBlock, @Nullable Entity originalEntity) {
        if (originalIsBlock) {
            Log.warn(LOG_TAG, "Storage open_variant '{}' is item_display but the holder is a block.", config.id());
            return null;
        }
        if (!(originalEntity instanceof ItemDisplay display) || !display.isValid()) {
            Log.warn(LOG_TAG, "Storage open_variant '{}' requires a valid ItemDisplay holder.", config.id());
            return null;
        }

        CustomStack openStack = CustomStack.getInstance(config.id());
        if (openStack == null) {
            Log.warn(LOG_TAG, "Could not resolve item_display open_variant '{}'.", config.id());
            return null;
        }

        ItemStack originalItem = display.getItemStack();
        state.savedItem(key, originalItem == null ? new ItemStack(Material.AIR) : originalItem.clone());
        display.setItemStack(openStack.getItemStack());
        state.liveEntity(key, display);
        return display;
    }

    @Nullable
    private Entity spawnOpenFurniture(
            Location location,
            BlockCoord key,
            boolean originalIsBlock,
            @Nullable Entity originalEntity
    ) {
        float yaw = captureAndClearOriginalHolder(location, key, originalIsBlock, originalEntity);
        Entity entity = OpenVariantPlacement.spawnFurniture(
                config.id(), location, originalIsBlock, originalIsBlock ? null : yaw);
        if (entity != null) {
            state.liveEntity(key, entity);
        }
        return entity;
    }

    @Nullable
    private Entity placeOpenBlock(
            Location location,
            BlockCoord key,
            boolean originalIsBlock,
            @Nullable Entity originalEntity,
            String rotationSuffix
    ) {
        captureAndClearOriginalHolder(location, key, originalIsBlock, originalEntity);
        String blockId = OpenVariantPlacement.rotatedVariantId(config.id(), rotationSuffix);
        OpenVariantPlacement.placeBlock(blockId, location);
        return null;
    }

    private float captureAndClearOriginalHolder(
            Location location,
            BlockCoord key,
            boolean originalIsBlock,
            @Nullable Entity originalEntity
    ) {
        if (originalIsBlock) {
            if (config.isFurnitureBased()) {
                OpenVariantPlacement.clearBlock(location);
            }
            return 0f;
        }

        if (originalEntity == null || !originalEntity.isValid()) {
            Log.warn(LOG_TAG, "Could not clear invalid original furniture at {}.", location);
            return 0f;
        }

        float yaw = originalEntity.getLocation().getYaw();
        state.savedYaw(key, yaw);
        OpenVariantPlacement.removeFurnitureEntity(originalEntity);
        return yaw;
    }

    @Nullable
    private Entity restoreItemDisplayModel(BlockCoord key) {
        Entity entity = state.removeLiveEntity(key);
        ItemStack savedItem = state.removeSavedItem(key);
        if (!(entity instanceof ItemDisplay display) || !display.isValid()) {
            Log.warn(LOG_TAG, "Could not restore item_display open_variant '{}': entity is gone.", config.id());
            return null;
        }
        if (savedItem == null) {
            Log.warn(LOG_TAG, "Could not restore item_display open_variant '{}': original item was not saved.", config.id());
            return display;
        }

        display.setItemStack(savedItem);
        return display;
    }

    private void removeOpenVariant(Location location, BlockCoord key) {
        if (config.isFurnitureBased()) {
            Entity entity = state.removeLiveEntity(key);
            if (entity != null && entity.isValid()) {
                OpenVariantPlacement.removeFurnitureEntity(entity);
            }
            return;
        }

        OpenVariantPlacement.clearBlock(location);
    }
}
