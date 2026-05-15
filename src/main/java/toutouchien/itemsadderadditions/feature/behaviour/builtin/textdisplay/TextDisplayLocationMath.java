package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;

@NullMarked
public final class TextDisplayLocationMath {
    private TextDisplayLocationMath() {
        throw new IllegalStateException("Utility class");
    }

    public static Location applyLocalOffset(Location base, float yaw, Vector localOffset) {
        Vector rotated = rotateY(localOffset, yaw);
        return base.clone().add(rotated);
    }

    public static Vector rotateY(Vector input, float yawDegrees) {
        double radians = Math.toRadians(yawDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double x = input.getX() * cos - input.getZ() * sin;
        double z = input.getX() * sin + input.getZ() * cos;
        return new Vector(x, input.getY(), z);
    }

    public static float blockYaw(String placedNamespacedId, float playerYaw) {
        String lower = placedNamespacedId.toLowerCase(Locale.ROOT);
        if (lower.endsWith("_south")) return 0.0F;
        if (lower.endsWith("_west")) return 90.0F;
        if (lower.endsWith("_north")) return 180.0F;
        if (lower.endsWith("_east")) return -90.0F;
        return snapYaw(playerYaw);
    }

    public static float snapYaw(float yaw) {
        float snapped = Math.round(yaw / 90.0F) * 90.0F;
        while (snapped > 180.0F) snapped -= 360.0F;
        while (snapped <= -180.0F) snapped += 360.0F;
        return snapped;
    }
}
