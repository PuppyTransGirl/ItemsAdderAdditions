package toutouchien.itemsadderadditions.actions.executors;

import dev.lone.itemsadder.api.CustomComplexFurniture;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.TriggerType;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;

/**
 * Plays an ItemsAdder animation on a complex furniture entity.
 * Restricted to {@link TriggerType#COMPLEX_FURNITURE_INTERACT} only.
 * <p>
 * Example:
 * <pre>{@code
 * play_animation:
 *   name: "wave"   # required
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "play_animation", triggers = {TriggerType.COMPLEX_FURNITURE_INTERACT})
public final class PlayAnimationAction extends ActionExecutor {
    @Parameter(key = "name", type = String.class, required = true)
    private String animationName;

    @Override
    protected void execute(ActionContext context) {
        CustomComplexFurniture entity = context.complexFurniture();
        if (entity == null)
            return;

        entity.getCustomEntity().playAnimation(animationName);
    }
}
