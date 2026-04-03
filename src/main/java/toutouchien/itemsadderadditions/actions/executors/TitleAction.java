package toutouchien.itemsadderadditions.actions.executors;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.hook.PlaceholderAPIUtils;

import java.time.Duration;

/**
 * Sends a title + subtitle with MiniMessage formatting.
 * Both {@code title} and {@code subtitle} are optional; omitting one sends an empty component.
 * <p>
 * Example:
 * <pre>{@code
 * title:
 *   title:    "<bold><gold>Welcome!"   # Optional
 *   subtitle: "<gray>Enjoy your stay"  # Optional
 *   fade_in:  5    # Optional (default: 10 ticks)
 *   stay:     40   # Optional (default: 70 ticks)
 *   fade_out: 30   # Optional (default: 20 ticks)
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "title")
public final class TitleAction extends ActionExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Parameter(key = "title", type = String.class, required = true)
    private String title;

    @Parameter(key = "subtitle", type = String.class, required = true)
    private String subtitle;

    @Parameter(key = "fade_in", type = Integer.class, min = 0, max = 1200)
    private Integer fadeIn = 10;

    @Parameter(key = "stay", type = Integer.class, min = 1, max = 1200)
    private Integer stay = 70;

    @Parameter(key = "fade_out", type = Integer.class, min = 0, max = 1200)
    private Integer fadeOut = 20;

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        Component titleComp = parse(runOn, title);
        Component subtitleComp = parse(runOn, subtitle);

        Title.Times times = Title.Times.times(
                duration(fadeIn),
                duration(stay),
                duration(fadeOut)
        );

        runOn.showTitle(Title.title(titleComp, subtitleComp, times));
    }

    private Duration duration(Integer ticks) {
        return Duration.ofMillis(ticks * 50L);
    }

    private Component parse(Entity runOn, String text) {
        String input = runOn instanceof Player player ? PlaceholderAPIUtils.parsePlaceholders(player, text) : text;
        return FontImageWrapper.replaceFontImages(MM.deserialize(input));
    }
}
