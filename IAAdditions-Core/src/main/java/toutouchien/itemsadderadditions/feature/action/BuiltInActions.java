package toutouchien.itemsadderadditions.feature.action;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.action.builtin.*;

import java.util.List;

/**
 * Built-in action prototypes registered by this plugin.
 */
@NullMarked
final class BuiltInActions {
    private BuiltInActions() {
    }

    static List<ActionExecutor> create() {
        return List.of(
                new ActionBarAction(),
                new ClearItemAction(),
                new IgniteAction(),
                new MessageAction(),
                new MythicMobsSkillAction(),
                new OpenInventoryAction(),
                new PlayAnimationAction(),
                new PlayEmoteAction(),
                new ReplaceBiomeAction(),
                new ShootFireballAction(),
                new SwingHandAction(),
                new TeleportAction(),
                new TitleAction(),
                new ToastAction(),
                new VeinminerAction()
        );
    }
}
