package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

@NullMarked
public final class OpenVariantPlacement {
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

    public static void removeFurnitureEntity(Entity entity) {
        Log.debug(LOG_TAG, "removeFurnitureEntity: entity={}, type={}, valid={}, loc={}",
                entity, entity.getType(), entity.isValid(), entity.getLocation());

        CustomFurniture furniture;
        try {
            furniture = CustomFurniture.byAlreadySpawned(entity);
        } catch (RuntimeException e) {
            Log.debug(LOG_TAG, "removeFurnitureEntity: CustomFurniture lookup failed for {}. Falling back to entity.remove(). Cause: {}",
                    entity, e.getMessage());
            entity.remove();
            return;
        }

        if (furniture != null) {
            Log.debug(LOG_TAG, "removeFurnitureEntity: resolved CustomFurniture - calling remove(false) to clean barriers.");
            furniture.remove(false);
            Log.debug(LOG_TAG, "removeFurnitureEntity: CustomFurniture.remove(false) returned. Entity valid={}, dead={}",
                    entity.isValid(), entity.isDead());
            return;
        }
        Log.debug(LOG_TAG, "removeFurnitureEntity: CustomFurniture.byAlreadySpawned returned null - falling back to entity.remove(). Barriers may remain.");
        entity.remove();
    }

    static void clearBlock(Location location) {
        location.getBlock().setType(Material.AIR);
    }

    /**
     * Schedules a delayed sweep around {@code center} (1-block radius cube) that clears
     * any leftover BARRIER blocks. Use after IA's furniture break flow so we run after
     * IA's own (sometimes incomplete) cleanup.
     */
    public static void scheduleBarrierSweep(JavaPlugin plugin, Location center, int radius) {
        plugin.getServer().getScheduler().runTask(plugin, () -> sweepBarriers(center, radius));
    }

    private static void sweepBarriers(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            Log.debug(LOG_TAG, "Barrier sweep: world is null at {}.", center);
            return;
        }

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int cleared = 0;
        int scanned = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    scanned++;
                    if (block.getType() != Material.BARRIER) continue;

                    Log.debug(LOG_TAG, "Barrier sweep: clearing BARRIER at {},{},{}.",
                            block.getX(), block.getY(), block.getZ());
                    block.setType(Material.AIR);
                    cleared++;
                }
            }
        }
        Log.debug(LOG_TAG, "Barrier sweep around ({},{},{}) radius={}: scanned={}, cleared={}.",
                cx, cy, cz, radius, scanned, cleared);
    }

    private static Block supportBlock(Location location, boolean replacingBlockHolder) {
        return replacingBlockHolder
                ? location.clone().subtract(0, 1, 0).getBlock()
                : location.getBlock();
    }

    record BlockState(@Nullable String id, String rotationSuffix) {}
}
