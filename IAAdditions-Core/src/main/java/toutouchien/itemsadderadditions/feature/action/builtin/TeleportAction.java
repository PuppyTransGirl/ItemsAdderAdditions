package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

/**
 * Teleports the player to a specified location.
 *
 * <pre>{@code
 * teleport:
 *   x: 100.5
 *   y: 64.0
 *   z: -50.5
 *   yaw:   90.0       # Optional - defaults to the player's current yaw
 *   pitch:  0.0       # Optional - defaults to the player's current pitch
 *   world: "world_nether"  # Optional - defaults to the player's current world
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "teleport")
public final class TeleportAction extends ActionExecutor {
    @Parameter(key = "x", type = Double.class, required = true)
    private Double x;

    @Parameter(key = "y", type = Double.class, required = true)
    private Double y;

    @Parameter(key = "z", type = Double.class, required = true)
    private Double z;

    @Parameter(key = "yaw", type = Float.class, min = -180.0, max = 180.0)
    @Nullable private Float yaw;

    @Parameter(key = "pitch", type = Float.class, min = -90.0, max = 90.0)
    @Nullable private Float pitch;

    @Parameter(key = "world", type = String.class)
    @Nullable private String world;

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        Location current = runOn.getLocation();

        float finalYaw = yaw != null ? yaw : current.getYaw();
        float finalPitch = pitch != null ? pitch : current.getPitch();
        String worldName = world != null ? world : runOn.getWorld().getName();

        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null) {
            Log.itemWarn("Actions", context.player().getName(), "teleport: world '{}' does not exist - skipping", worldName);
            return;
        }

        runOn.teleportAsync(new Location(bukkitWorld, x, y, z, finalYaw, finalPitch));
    }
}
