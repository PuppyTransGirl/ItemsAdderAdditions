package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;

/**
 * Utilities for positioning text displays relative to their owner's location and facing.
 */
@NullMarked
public final class TextDisplayLocationMath {
    private TextDisplayLocationMath() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Rotates {@code localOffset} by the owner's yaw, then adds it to {@code base}.
     * This keeps the offset aligned to the owner's facing direction regardless of rotation.
     */
    public static Location applyLocalOffset(Location base, float yaw, Vector localOffset) {
        Vector rotated = rotateY(localOffset, yaw);
        return base.clone().add(rotated);
    }

    /** Rotates a vector around the Y axis by the given yaw in degrees. */
    public static Vector rotateY(Vector input, float yawDegrees) {
        double radians = Math.toRadians(yawDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double x = input.getX() * cos - input.getZ() * sin;
        double z = input.getX() * sin + input.getZ() * cos;
        return new Vector(x, input.getY(), z);
    }

    /**
     * Returns the yaw for a placed block by reading the directional suffix from its namespaced ID
     * (e.g. {@code _south}, {@code _west}). Falls back to snapping the player's yaw when
     * no suffix is present.
     */
    public static float blockYaw(String placedNamespacedId, float playerYaw) {
        String lower = placedNamespacedId.toLowerCase(Locale.ROOT);
        if (lower.endsWith("_south")) return 0.0F;
        if (lower.endsWith("_west")) return 90.0F;
        if (lower.endsWith("_north")) return 180.0F;
        if (lower.endsWith("_east")) return -90.0F;
        return snapYaw(playerYaw);
    }

    /** Rounds a yaw to the nearest 90-degree step, keeping the result in (-180, 180]. */
    public static float snapYaw(float yaw) {
        float snapped = Math.round(yaw / 90.0F) * 90.0F;
        while (snapped > 180.0F) snapped -= 360.0F;
        while (snapped <= -180.0F) snapped += 360.0F;
        return snapped;
    }
}
