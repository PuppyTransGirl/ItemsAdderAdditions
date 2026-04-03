package toutouchien.itemsadderadditions.actions.executors;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.hook.MythicMobsUtils;

/**
 * Sends a message with MiniMessage support.
 * <p>
 * Example:
 * <pre>{@code
 * mythic_mobs_skill:
 *   skill: "duplicate"
 *   power: 0.5 # Optional (default: 1.0)
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "mythic_mobs_skill")
public final class MythicMobsSkillAction extends ActionExecutor {
    @Parameter(key = "skill", type = String.class, required = true)
    private String skill;

    @Parameter(key = "power", type = Float.class)
    private float power = 1F;

    @Override
    protected void execute(ActionContext context) {
        MythicMobsUtils.castSkill(context.player(), skill, power);
    }
}
