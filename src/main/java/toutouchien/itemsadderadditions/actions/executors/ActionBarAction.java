package toutouchien.itemsadderadditions.actions.executors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.PlaceholderAPIUtils;

/**
 * Sends an action bar message with MiniMessage support.
 * <p>
 * Example:
 * <pre>{@code
 * actionbar:
 *   text: "<yellow>You right-clicked me!"
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "actionbar")
public final class ActionBarAction extends ActionExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Parameter(key = "text", type = String.class, required = true)
    private String text;

    @Override
    protected void execute(ActionContext context) {
        Player player = context.player();
        Component message = MM.deserialize(PlaceholderAPIUtils.parsePlaceholders(player, text));
        player.sendActionBar(message);
    }
}
