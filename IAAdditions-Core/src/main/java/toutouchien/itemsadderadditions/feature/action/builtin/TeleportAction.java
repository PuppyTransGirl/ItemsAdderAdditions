package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

/**
 * Teleports the targeted entity to absolute/relative coordinates or a named destination.
 *
 * <pre>{@code
 * # Absolute coordinates
 * teleport:
 *   x: 100
 *   y: 64
 *   z: -50
 *
 * # Relative coordinates (from the targeted entity)
 * teleport:
 *   x: "~10"
 *   y: "~"
 *   z: "~-5"
 *
 * # Mixed absolute and relative coordinates
 * teleport:
 *   x: 100
 *   y: "~5"
 *   z: "~-2"
 *
 * # Named destinations
 * teleport:
 *   destination: world_spawn
 *   world: "world_nether" # Optional
 *
 * teleport:
 *   destination: respawn
 *   yaw: 90.0             # Optional absolute override
 *   pitch: 0.0            # Optional absolute override
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "teleport")
public final class TeleportAction extends ActionExecutor {
    private Coordinate x = new Coordinate(0, false);
    private Coordinate y = new Coordinate(0, false);
    private Coordinate z = new Coordinate(0, false);

    @Parameter(key = "yaw", type = Float.class, min = -180.0, max = 180.0)
    @Nullable private Float yaw;

    @Parameter(key = "pitch", type = Float.class, min = -90.0, max = 90.0)
    @Nullable private Float pitch;

    @Parameter(key = "world", type = String.class)
    @Nullable private String world;

    @Parameter(key = "destination", type = String.class)
    @Nullable private String destination;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!super.configure(configData, namespacedID))
            return false;

        if (!(configData instanceof ConfigurationSection section))
            return invalid(namespacedID, "coordinates or a destination are required");

        Object rawX = section.get("x");
        Object rawY = section.get("y");
        Object rawZ = section.get("z");
        boolean hasCoordinates = section.contains("x") || section.contains("y") || section.contains("z");
        if (section.contains("destination")) {
            if (hasCoordinates)
                return invalid(namespacedID, "destination cannot be combined with x, y, or z");
            if (!"world_spawn".equals(destination) && !"respawn".equals(destination))
                return invalid(namespacedID, "unknown destination '" + destination
                        + "' (expected 'world_spawn' or 'respawn')");
            return true;
        }

        if (rawX == null || rawY == null || rawZ == null) {
            return invalid(namespacedID, "x, y, and z must all be configured");
        }

        try {
            x = Coordinate.parse(rawX);
            y = Coordinate.parse(rawY);
            z = Coordinate.parse(rawZ);
            return true;
        } catch (IllegalArgumentException exception) {
            return invalid(namespacedID,
                    "x, y, and z must be finite numbers or relative coordinates such as '~' or '~-2.5'");
        }
    }

    private static boolean invalid(String namespacedID, String reason) {
        Log.itemSkip("Actions", namespacedID, "teleport: " + reason);
        return false;
    }

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        Location current = runOn.getLocation();
        Location target = destination == null
                ? coordinates(context, current)
                : namedDestination(context, runOn);
        if (target == null)
            return;

        if (yaw != null)
            target.setYaw(yaw);
        if (pitch != null)
            target.setPitch(pitch);
        runOn.teleportAsync(target);
    }

    @Nullable
    private Location coordinates(ActionContext context, Location current) {
        World targetWorld = world(context, context.runOn().getWorld());
        return targetWorld == null ? null : new Location(
                targetWorld,
                x.resolve(current.getX()),
                y.resolve(current.getY()),
                z.resolve(current.getZ()),
                current.getYaw(),
                current.getPitch()
        );
    }

    @Nullable
    private Location namedDestination(ActionContext context, Entity runOn) {
        if ("respawn".equals(destination)) {
            Location respawn = context.player().getRespawnLocation();
            if (respawn != null)
                return respawn.clone();
            World fallback = world(context, context.player().getWorld());
            return fallback == null ? null : fallback.getSpawnLocation();
        }

        World spawnWorld = world(context, runOn.getWorld());
        return spawnWorld == null ? null : spawnWorld.getSpawnLocation();
    }

    @Nullable
    private World world(ActionContext context, World fallback) {
        if (world == null)
            return fallback;

        World configured = Bukkit.getWorld(world);
        if (configured == null)
            Log.itemWarn("Actions", context.player().getName(),
                    "teleport: world '{}' does not exist - skipping", world);
        return configured;
    }

    private record Coordinate(double value, boolean relative) {
        private static Coordinate parse(Object raw) {
            if (raw instanceof Number number)
                return absolute(number.doubleValue());

            if (!(raw instanceof String text) || !text.startsWith("~")
                    || text.chars().anyMatch(Character::isWhitespace))
                throw new IllegalArgumentException();

            if (text.equals("~"))
                return new Coordinate(0, true);

            double offset = Double.parseDouble(text.substring(1));
            if (!Double.isFinite(offset))
                throw new IllegalArgumentException();
            return new Coordinate(offset, true);
        }

        private static Coordinate absolute(double value) {
            if (!Double.isFinite(value))
                throw new IllegalArgumentException();
            return new Coordinate(value, false);
        }

        private double resolve(double current) {
            return relative ? current + value : value;
        }
    }
}
