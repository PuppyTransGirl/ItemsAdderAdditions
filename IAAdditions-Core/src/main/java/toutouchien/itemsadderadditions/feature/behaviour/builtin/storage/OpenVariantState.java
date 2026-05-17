package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@NullMarked
final class OpenVariantState {
    private final Map<BlockCoord, Integer> openCounts = new HashMap<>();
    private final Map<BlockCoord, Entity> liveEntities = new HashMap<>();
    private final Map<BlockCoord, String> savedBlockIds = new HashMap<>();
    private final Map<BlockCoord, Float> savedYaws = new HashMap<>();
    private final Map<BlockCoord, ItemStack> savedItems = new HashMap<>();

    int increment(BlockCoord key) {
        return openCounts.merge(key, 1, Integer::sum);
    }

    int decrement(BlockCoord key) {
        int remaining = openCounts.merge(key, -1, Integer::sum);
        if (remaining <= 0) {
            openCounts.remove(key);
        }
        return remaining;
    }

    boolean isOpen(BlockCoord key) {
        return openCounts.getOrDefault(key, 0) > 0;
    }

    void liveEntity(BlockCoord key, Entity entity) {
        liveEntities.put(key, entity);
    }

    @Nullable
    Entity liveEntity(BlockCoord key) {
        return liveEntities.get(key);
    }

    @Nullable
    Entity removeLiveEntity(BlockCoord key) {
        return liveEntities.remove(key);
    }

    void savedBlockId(BlockCoord key, String blockId) {
        savedBlockIds.put(key, blockId);
    }

    @Nullable
    String removeSavedBlockId(BlockCoord key) {
        return savedBlockIds.remove(key);
    }

    void savedYaw(BlockCoord key, float yaw) {
        savedYaws.put(key, yaw);
    }

    @Nullable
    Float removeSavedYaw(BlockCoord key) {
        return savedYaws.remove(key);
    }

    void savedItem(BlockCoord key, ItemStack item) {
        savedItems.put(key, item);
    }

    @Nullable
    ItemStack removeSavedItem(BlockCoord key) {
        return savedItems.remove(key);
    }

    void forget(BlockCoord key) {
        openCounts.remove(key);
        savedBlockIds.remove(key);
        savedYaws.remove(key);
        savedItems.remove(key);
    }

    void clear() {
        liveEntities.values().stream()
                .filter(Entity::isValid)
                .forEach(Entity::remove);
        liveEntities.clear();
        openCounts.clear();
        savedBlockIds.clear();
        savedYaws.clear();
        savedItems.clear();
    }
}
