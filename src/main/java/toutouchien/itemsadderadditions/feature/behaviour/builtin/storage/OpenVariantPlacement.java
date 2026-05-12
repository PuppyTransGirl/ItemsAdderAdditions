package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

@NullMarked
final class OpenVariantPlacement {
    private static final String LOG_TAG = "OpenVariant";

    private OpenVariantPlacement() {
    }

    static BlockState captureBlockState(Location location) {
        CustomBlock block = CustomBlock.byAlreadyPlaced(location.getBlock());
        if (block == null) {
            return new BlockState(null, "");
        }

        String id = block.getNamespacedID();
        String base = NamespaceUtils.stripRotationSuffix(id);
        return new BlockState(id, id.substring(base.length()));
    }

    static String rotatedVariantId(String baseId, String suffix) {
        if (suffix.isEmpty()) {
            return baseId;
        }

        String rotatedId = baseId + suffix;
        return CustomBlock.getInstance(rotatedId) == null ? baseId : rotatedId;
    }

    static boolean placeBlock(String namespacedId, Location location) {
        CustomBlock block = CustomBlock.getInstance(namespacedId);
        if (block == null) {
            Log.warn(LOG_TAG, "Could not resolve block '{}' for open-variant placement.", namespacedId);
            return false;
        }

        block.place(location);
        return true;
    }

    @Nullable
    static Entity spawnFurniture(String namespacedId, Location location, boolean replacingBlockHolder, @Nullable Float yaw) {
        CustomFurniture furniture = CustomFurniture.spawn(namespacedId, supportBlock(location, replacingBlockHolder));
        if (furniture == null || furniture.getEntity() == null) {
            Log.warn(LOG_TAG, "Could not spawn furniture '{}' at {}.", namespacedId, location);
            return null;
        }

        Entity entity = furniture.getEntity();
        if (yaw != null) {
            entity.setRotation(yaw, 0f);
        }
        return entity;
    }

    static void removeFurnitureEntity(Entity entity) {
        CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entity);
        if (furniture != null) {
            furniture.remove(false);
            return;
        }
        entity.remove();
    }

    static void clearBlock(Location location) {
        location.getBlock().setType(Material.AIR);
    }

    private static Block supportBlock(Location location, boolean replacingBlockHolder) {
        return replacingBlockHolder
                ? location.clone().subtract(0, 1, 0).getBlock()
                : location.getBlock();
    }

    record BlockState(@Nullable String id, String rotationSuffix) {}
}
