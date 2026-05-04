package toutouchien.itemsadderadditions.actions.executors;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.TextRenderer;

/**
 * Sends an action bar message with MiniMessage and PlaceholderAPI support.
 *
 * <p>Example:
 * <pre>{@code
 * actionbar:
 *   text: "<yellow>You right-clicked me!"
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "actionbar")
public final class ActionBarAction extends ActionExecutor {
    @Parameter(key = "text", type = String.class, required = true)
    private String text;

    @Override
    protected void execute(ActionContext context) {
        context.runOn().sendActionBar(TextRenderer.render(context.runOn(), text));
    }
}
