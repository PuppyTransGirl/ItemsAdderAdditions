package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.contentvariant;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryManager;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantConfig;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantPlacement;

/** Applies the closed visual that corresponds to a storage inventory's fill state. */
@NullMarked
public final class ContentVariantTransformer {
    private static final String LOG_TAG = "ContentVariant";

    private final ContentVariantConfig config;
    private final String originalId;
    private final NamespacedKey markerKey;
    private final JavaPlugin plugin;

    public ContentVariantTransformer(
            ContentVariantConfig config,
            String originalId,
            NamespacedKey markerKey,
            JavaPlugin plugin
    ) {
        this.config = config;
        this.originalId = originalId;
        this.markerKey = markerKey;
        this.plugin = plugin;
    }

    public void applyToBlock(Block block, @Nullable ItemStack @Nullable [] contents) {
        OpenVariantConfig selected = config.variantFor(contents);
        String targetId = selected == null ? originalId : selected.id();
        OpenVariantPlacement.BlockState current = OpenVariantPlacement.captureBlockState(block.getLocation());
        String currentBaseId = current.id() == null ? "" : NamespaceUtils.stripRotationSuffix(current.id());

        if (!currentBaseId.equals(targetId)) {
            String rotatedTarget = OpenVariantPlacement.rotatedVariantId(targetId, current.rotationSuffix());
            if (!OpenVariantPlacement.placeBlock(rotatedTarget, block.getLocation())) {
                Log.warn(LOG_TAG, "Could not apply storage content variant '{}' at {}.", targetId, block.getLocation());
                return;
            }
        }

        if (selected == null) {
            StorageInventoryManager.clearContentVariantMarker(block, markerKey, plugin);
        } else {
            StorageInventoryManager.markContentVariant(block, markerKey, originalId, selected.id(), plugin);
        }
    }

    /** Returns the entity that must receive the persisted inventory contents. */
    @Nullable
    public Entity applyToEntity(
            Location location,
            @Nullable Entity currentEntity,
            @Nullable ItemStack @Nullable [] contents
    ) {
        OpenVariantConfig selected = config.variantFor(contents);
        String marker = currentEntity == null
                ? null
                : StorageInventoryManager.contentVariantMarker(currentEntity, markerKey);
        String markedVariant = StorageInventoryManager.contentVariantId(marker, originalId);

        if (selected == null) {
            if (markedVariant == null) return currentEntity;
            return spawnFurniture(originalId, location, currentEntity, false);
        }

        if (selected.id().equals(markedVariant) && currentEntity != null && currentEntity.isValid()) {
            return currentEntity;
        }

        if (selected.category() == ItemCategory.ITEM) {
            return applyItemModel(selected, currentEntity);
        }

        return spawnFurniture(selected.id(), location, currentEntity, true);
    }

    public boolean isMarkedBlock(Block block) {
        return StorageInventoryManager.isContentVariantMarker(
                StorageInventoryManager.contentVariantMarker(block, markerKey, plugin), originalId);
    }

    public boolean isMarkedEntity(Entity entity) {
        return StorageInventoryManager.isContentVariantMarker(
                StorageInventoryManager.contentVariantMarker(entity, markerKey), originalId);
    }

    @Nullable
    private Entity applyItemModel(OpenVariantConfig selected, @Nullable Entity currentEntity) {
        if (!(currentEntity instanceof ItemDisplay display) || !display.isValid()) {
            Log.warn(LOG_TAG, "Storage content variant '{}' requires a valid ItemDisplay holder.", selected.id());
            return currentEntity;
        }

        CustomStack stack = CustomStack.getInstance(selected.id());
        if (stack == null) {
            Log.warn(LOG_TAG, "Could not resolve item_display content variant '{}'.", selected.id());
            return currentEntity;
        }

        ItemStack item = stack.getItemStack();
        display.setItemStack(item == null ? ItemStack.of(Material.AIR) : item);
        StorageInventoryManager.markContentVariant(display, markerKey, originalId, selected.id());
        return display;
    }

    @Nullable
    private Entity spawnFurniture(
            String targetId,
            Location location,
            @Nullable Entity currentEntity,
            boolean markAsVariant
    ) {
        Float yaw = currentEntity == null ? null : currentEntity.getLocation().getYaw();
        if (currentEntity != null && currentEntity.isValid()) {
            OpenVariantPlacement.removeFurnitureEntity(currentEntity);
        }

        Entity replacement = OpenVariantPlacement.spawnFurniture(targetId, location, false, yaw);
        if (replacement == null && !targetId.equals(originalId)) {
            Log.warn(LOG_TAG, "Could not apply storage content variant '{}'; restoring '{}'.",
                    targetId, originalId);
            replacement = OpenVariantPlacement.spawnFurniture(originalId, location, false, yaw);
            markAsVariant = false;
        }
        if (replacement != null && markAsVariant) {
            StorageInventoryManager.markContentVariant(replacement, markerKey, originalId, targetId);
        }
        return replacement;
    }
}
