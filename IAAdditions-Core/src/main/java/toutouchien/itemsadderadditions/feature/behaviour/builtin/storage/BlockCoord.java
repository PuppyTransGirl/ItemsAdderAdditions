package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;

/**
 * Immutable block-coordinate key that excludes yaw and pitch.
 * <p>
 * {@link Location#hashCode()} includes yaw/pitch; furniture entity locations carry the
 * entity's facing yaw, so this record is used as a map key instead.
 */
@NullMarked
public record BlockCoord(String world, int x, int y, int z) {
    public static BlockCoord of(Location loc) {
        return new BlockCoord(
                loc.getWorld() == null ? "" : loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()
        );
    }
}
