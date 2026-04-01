package toutouchien.itemsadderadditions.actions.executors;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;

/**
 * Shoots a fireball from the entity's location toward its look direction.
 *
 * <pre>{@code
 * shoot_fireball:
 *   power: 1.0
 *   speed: 1.0 # Optional - velocity multiplier (default: 1.0)
 *   fire: false # Optional - whether it sets blocks on fire (default: true)
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "shoot_fireball")
public final class ShootFireballAction extends ActionExecutor {
    @Parameter(key = "power", type = Float.class, required = true, min = 0, max = 127)
    private float power;

    @Parameter(key = "speed", type = Double.class, min = -1_000_000, max = 1_000_000)
    private double speed = 1D;

    @Parameter(key = "fire", type = Boolean.class)
    private boolean fire = true;

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        if (!(runOn instanceof LivingEntity livingEntity))
            return;

        Vector direction = livingEntity.getLocation().getDirection();
        livingEntity.getWorld().spawn(
                livingEntity.getEyeLocation(),
                Fireball.class,
                CreatureSpawnEvent.SpawnReason.CUSTOM,
                fireball -> {
                    fireball.setShooter(livingEntity);
                    fireball.setDirection(direction);

                    fireball.setYield(power);
                    fireball.setVelocity(direction.clone().multiply(speed));
                    fireball.setIsIncendiary(fire);
                }
        );
    }
}
