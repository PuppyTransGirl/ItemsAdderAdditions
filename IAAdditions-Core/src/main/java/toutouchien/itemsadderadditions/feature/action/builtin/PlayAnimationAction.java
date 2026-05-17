package toutouchien.itemsadderadditions.feature.action.builtin;

import dev.lone.itemsadder.api.CustomEntity;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.TriggerType;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

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
        Entity entity = context.complexFurniture();
        if (entity == null)
            return;

        CustomEntity customEntity = CustomEntity.byAlreadySpawned(entity);
        if (customEntity == null)
            return;

        customEntity.playAnimation(animationName);
    }
}
