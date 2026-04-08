package toutouchien.itemsadderadditions.behaviours.executors.storage;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Immutable snapshot of a single open storage GUI session.
 *
 * <p>Exactly one of {@code block} or {@code entity} will be non-null, depending on whether
 * the player opened a block-based or furniture-based storage.
 *
 * @param player    The player whose inventory is currently open.
 * @param inventory The Bukkit {@link Inventory} instance shown to the player.
 * @param block     The backing block, or {@code null} if this is a furniture session.
 * @param entity    The backing furniture entity, or {@code null} if this is a block session.
 * @param type      The {@link StorageType} of this session.
 */
@NullMarked
public record StorageSession(
        Player player,
        Inventory inventory,
        @Nullable Block block,
        @Nullable Entity entity,
        StorageType type
) {
    /**
     * Returns the world location of the backing holder (block or entity).
     * Used for proximity checks when a block/furniture is broken while a GUI is open.
     */
    public Location holderLocation() {
        if (block != null)
            return block.getLocation();

        if (entity != null)
            return entity.getLocation();

        throw new IllegalStateException("StorageSession has neither a block nor an entity");
    }

    /**
     * Returns {@code true} if this session is backed by a block.
     */
    public boolean isBlock() {
        return block != null;
    }

    /**
     * Returns {@code true} if this session is backed by a furniture entity.
     */
    public boolean isFurniture() {
        return entity != null;
    }
}
