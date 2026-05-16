package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.item.ItemCategory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Identifies a specific block or furniture entity that has text displays attached to it.
 * <p>
 * The {@code ownerId} is stable across reloads: blocks use a deterministic UUID derived
 * from their coordinates, furniture entities use the entity UUID directly.
 */
@NullMarked
public record TextDisplayOwner(
        UUID ownerId,
        String namespacedId,
        ItemCategory category,
        Location baseLocation,
        float yaw
) {
    public TextDisplayOwner {
        baseLocation = baseLocation.clone();
    }

    /**
     * Creates an owner for a custom block, with the base location centered on the block's XZ.
     */
    public static TextDisplayOwner block(String namespacedId, Block block, float yaw) {
        Location base = block.getLocation().add(0.5, 0.0, 0.5);
        return new TextDisplayOwner(
                blockOwnerId(block.getLocation()),
                namespacedId,
                ItemCategory.BLOCK,
                base,
                yaw
        );
    }

    /** Creates an owner for a furniture entity. */
    public static TextDisplayOwner furniture(String namespacedId, ItemCategory category, Entity entity) {
        Location location = entity.getLocation();
        return new TextDisplayOwner(
                entity.getUniqueId(),
                namespacedId,
                category,
                location,
                location.getYaw()
        );
    }

    /**
     * Derives a deterministic UUID for a block location by hashing the world UID and block
     * coordinates. The result is stable across server restarts for the same block position.
     */
    public static UUID blockOwnerId(Location blockLocation) {
        World world = blockLocation.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Block owner location has no world");
        }

        String key = world.getUID()
                + ":" + blockLocation.getBlockX()
                + ":" + blockLocation.getBlockY()
                + ":" + blockLocation.getBlockZ();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}
