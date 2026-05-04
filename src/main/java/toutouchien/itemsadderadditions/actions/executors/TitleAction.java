package toutouchien.itemsadderadditions.actions.executors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.TextRenderer;

import java.time.Duration;

/**
 * Sends a title + subtitle with MiniMessage, PlaceholderAPI, and font-image support.
 *
 * <p>Both {@code title} and {@code subtitle} are optional - omitting either sends
 * an empty component for that slot. You must provide at least one of them to have
 * any visible effect.
 *
 * <p>Example:
 * <pre>{@code
 * title:
 *   title:    "<bold><gold>Welcome!"   # optional
 *   subtitle: "<gray>Enjoy your stay"  # optional
 *   fade_in:  5    # optional, default: 10 ticks
 *   stay:     40   # optional, default: 70 ticks
 *   fade_out: 30   # optional, default: 20 ticks
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "title")
public final class TitleAction extends ActionExecutor {
    /**
     * The main title text (MiniMessage). {@code null} when omitted → empty component.
     */
    @Parameter(key = "title", type = String.class)
    @Nullable private String title;

    /**
     * The subtitle text (MiniMessage). {@code null} when omitted → empty component.
     */
    @Parameter(key = "subtitle", type = String.class)
    @Nullable private String subtitle;

    @Parameter(key = "fade_in", type = Integer.class, min = 0, max = 1200)
    private int fadeIn = 10;

    @Parameter(key = "stay", type = Integer.class, min = 1, max = 1200)
    private int stay = 70;

    @Parameter(key = "fade_out", type = Integer.class, min = 0, max = 1200)
    private int fadeOut = 20;

    private static Duration ticksToDuration(int ticks) {
        return Duration.ofMillis(ticks * 50L);
    }

    @Override
    protected void execute(ActionContext context) {
        Component titleComp = title != null
                ? TextRenderer.render(context.runOn(), title)
                : Component.empty();
        Component subtitleComp = subtitle != null
                ? TextRenderer.render(context.runOn(), subtitle)
                : Component.empty();

        Title.Times times = Title.Times.times(ticksToDuration(fadeIn), ticksToDuration(stay), ticksToDuration(fadeOut));
        context.runOn().showTitle(Title.title(titleComp, subtitleComp, times));
    }
}
