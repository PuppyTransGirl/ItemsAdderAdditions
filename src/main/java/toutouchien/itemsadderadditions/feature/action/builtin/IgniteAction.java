package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

/**
 * Set an entity on fire.
 * <p>
 * Example:
 * <pre>{@code
 * ignite:
 *   duration: 200
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "ignite")
public final class IgniteAction extends ActionExecutor {
    @Parameter(key = "duration", type = Integer.class, required = true, min = 1, max = 72_000) // 72 000 = 1h
    private int duration;

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        runOn.setFireTicks(duration);
    }
}
