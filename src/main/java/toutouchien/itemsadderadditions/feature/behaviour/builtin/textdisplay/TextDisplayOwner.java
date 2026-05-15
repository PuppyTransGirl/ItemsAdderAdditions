package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.item.ItemCategory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
