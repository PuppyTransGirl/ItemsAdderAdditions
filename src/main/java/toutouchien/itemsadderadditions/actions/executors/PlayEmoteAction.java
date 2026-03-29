package toutouchien.itemsadderadditions.actions.executors;

import dev.lone.itemsadder.api.CustomPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;

/**
 * Plays an ItemsAdder emote.
 * <p>
 * Example:
 * <pre>{@code
 * play_emote:
 *   name: "wave"   # required
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "play_emote")
public final class PlayEmoteAction extends ActionExecutor {
    @Parameter(key = "name", type = String.class, required = true)
    private String emoteName;

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        if (!(runOn instanceof Player player))
            return;

        CustomPlayer.playEmote(player, emoteName);
    }
}
