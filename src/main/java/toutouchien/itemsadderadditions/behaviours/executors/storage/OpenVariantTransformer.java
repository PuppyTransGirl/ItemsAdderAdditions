package toutouchien.itemsadderadditions.behaviours.executors.storage;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.HashMap;
import java.util.Map;

@NullMarked
public final class OpenVariantTransformer {
    private final OpenVariantConfig config;

    private final Map<BlockCoord, Integer> openCounts = new HashMap<>();
    private final Map<BlockCoord, Entity> liveEntities = new HashMap<>();

    public OpenVariantTransformer(OpenVariantConfig config) {
        this.config = config;
    }

    @Nullable
    public Entity onFirstOpen(
            Location loc,
            boolean isBlock,
            @Nullable Entity originalEntity
    ) {
        BlockCoord key = BlockCoord.of(loc);
        int prev = openCounts.merge(key, 1, Integer::sum) - 1;

        if (prev > 0)
            return liveEntities.get(key);

        return applyTransform(loc, key, isBlock, originalEntity);
    }

    @Nullable
    public Entity onLastClose(Location loc, String originalId, boolean isBlock) {
        BlockCoord key = BlockCoord.of(loc);
        int remaining = openCounts.merge(key, -1, Integer::sum);

        if (remaining > 0)
            return null;

        openCounts.remove(key);
        return restoreTransform(loc, key, originalId, isBlock);
    }

    public boolean isTransformed(Location loc) {
        return openCounts.getOrDefault(BlockCoord.of(loc), 0) > 0;
    }

    public void forceRemove(Location loc) {
        BlockCoord key = BlockCoord.of(loc);
        openCounts.remove(key);

        Entity entity = liveEntities.remove(key);
        if (entity != null && entity.isValid())
            entity.remove();
    }

    public void clear() {
        liveEntities.values().stream()
                .filter(Entity::isValid)
                .forEach(Entity::remove);

        liveEntities.clear();
        openCounts.clear();
    }

    @Nullable
    private Entity applyTransform(
            Location loc,
            BlockCoord key,
            boolean isBlock,
            @Nullable Entity originalEntity
    ) {
        // BLOCK → FURNITURE case still needs clearing
        clearHolder(loc, isBlock, originalEntity);

        if (config.isFurnitureBased()) {
            if (!isBlock) {
                // ✅ BEST CASE: replace existing furniture (keeps rotation)
                if (originalEntity == null || !originalEntity.isValid()) {
                    Log.warn(
                            "OpenVariantTransformer",
                            "Expected valid furniture entity at {} but got null/invalid.",
                            loc
                    );
                    return null;
                }

                CustomFurniture furniture = CustomFurniture.byAlreadySpawned(originalEntity);
                if (furniture == null) {
                    Log.warn(
                            "OpenVariantTransformer",
                            "Could not resolve CustomFurniture from entity at {}.",
                            loc
                    );
                    return null;
                }

                furniture.replaceFurniture(config.id());

                Entity entity = furniture.getEntity();
                if (entity != null)
                    liveEntities.put(key, entity);

                return entity;
            }

            // BLOCK → FURNITURE (no existing entity to replace)
            Block supportBlock = furnitureSupportBlock(loc, true);
            CustomFurniture spawned = CustomFurniture.spawn(config.id(), supportBlock);

            if (spawned == null || spawned.getEntity() == null) {
                Log.warn(
                        "OpenVariantTransformer",
                        "Failed to spawn open-form furniture '{}' at {}.",
                        config.id(),
                        loc
                );
                return null;
            }

            Entity entity = spawned.getEntity();
            liveEntities.put(key, entity);
            return entity;
        }

        // BLOCK → BLOCK
        if (!placeBlock(config.id(), loc)) {
            Log.warn(
                    "OpenVariantTransformer",
                    "Failed to place open-form block '{}' at {}.",
                    config.id(),
                    loc
            );
        }

        return null;
    }

    @Nullable
    private Entity restoreTransform(
            Location loc,
            BlockCoord key,
            String originalId,
            boolean originalIsBlock
    ) {
        if (config.isFurnitureBased()) {
            Entity entity = liveEntities.remove(key);

            if (entity != null && entity.isValid()) {
                CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entity);

                if (furniture != null) {
                    // Replace back → keeps rotation automatically
                    furniture.replaceFurniture(originalId);
                    return furniture.getEntity();
                }

                entity.remove(); // fallback safety
            }
            return null;
        }

        clearOpenVariant(loc, key, originalIsBlock);

        if (originalIsBlock) {
            if (!placeBlock(originalId, loc)) {
                Log.warn(
                        "OpenVariantTransformer",
                        "Failed to restore original block '{}' at {}.",
                        originalId,
                        loc
                );
            }
            return null;
        }

        // BLOCK → FURNITURE restore
        Block supportBlock = furnitureSupportBlock(loc, false);
        CustomFurniture restored = CustomFurniture.spawn(originalId, supportBlock);

        if (restored == null || restored.getEntity() == null) {
            Log.warn(
                    "OpenVariantTransformer",
                    "Failed to restore original furniture '{}' at {}.",
                    originalId,
                    loc
            );
            return null;
        }

        return restored.getEntity();
    }

    private void clearHolder(
            Location loc,
            boolean isBlock,
            @Nullable Entity originalEntity
    ) {
        if (!isBlock)
            return;

        if (config.isFurnitureBased()) {
            loc.getBlock().setType(Material.AIR);
        }
    }

    private void clearOpenVariant(
            Location loc,
            BlockCoord key,
            boolean originalIsBlock
    ) {
        if (config.isFurnitureBased())
            return; // handled by replaceFurniture

        if (!originalIsBlock)
            loc.getBlock().setType(Material.AIR);
    }

    private Block furnitureSupportBlock(Location loc, boolean replacingBlockHolder) {
        return replacingBlockHolder
                ? loc.clone().subtract(0, 1, 0).getBlock()
                : loc.getBlock();
    }

    private boolean placeBlock(String namespacedId, Location loc) {
        CustomBlock cb = CustomBlock.getInstance(namespacedId);
        if (cb == null)
            return false;

        cb.place(loc);
        return true;
    }
}
