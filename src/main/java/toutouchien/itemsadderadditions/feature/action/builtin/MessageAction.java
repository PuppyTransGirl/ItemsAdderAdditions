package toutouchien.itemsadderadditions.feature.action.builtin;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.utils.TextRenderer;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

/**
 * Sends a chat message with MiniMessage and PlaceholderAPI support.
 *
 * <p>Example:
 * <pre>{@code
 * message:
 *   text: "<yellow>You right-clicked me!"
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "message")
public final class MessageAction extends ActionExecutor {
    @Parameter(key = "text", type = String.class, required = true)
    private String text;

    @Override
    protected void execute(ActionContext context) {
        context.runOn().sendMessage(TextRenderer.render(context.runOn(), text));
    }
}
