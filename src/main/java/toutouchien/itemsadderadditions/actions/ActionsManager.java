package toutouchien.itemsadderadditions.actions;

import dev.lone.itemsadder.api.CustomStack;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.actions.executors.*;
import toutouchien.itemsadderadditions.actions.loading.ActionLoader;
import toutouchien.itemsadderadditions.utils.other.ExecutorRegistry;

import java.util.List;

/**
 * Entry point for the actions system.
 *
 * <p>Instantiate once during plugin enable; then call {@link #reload(List)} ()} after
 * ItemsAdder fires {@code ItemsAdderLoadDataEvent} so all custom items are available.
 *
 * <h3>Registering custom actions</h3>
 * Call {@link #registry()}{@code .register(...)} before the first reload,
 * either here in the constructor (for built-in actions) or from an external plugin's
 * {@code onEnable}.
 *
 * <h3>Disabling built-in actions</h3>
 * Each built-in action can be disabled in {@code config.yml} under the
 * {@code actions} section:
 * <pre>{@code
 * actions:
 *   veinminer:      false
 *   shoot_fireball: false
 * }</pre>
 */
public final class ActionsManager {
    private static final String CONFIG_PREFIX = "actions.";

    private final ExecutorRegistry<ActionExecutor> registry = new ExecutorRegistry<>("Actions");
    private final ActionLoader loader = new ActionLoader(registry);

    public ActionsManager() {
        registry.registerIfEnabled(
                ItemsAdderAdditions.instance().getConfig(),
                CONFIG_PREFIX,
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

    /**
     * (Re)loads every item's actions from its YAML config using a pre-fetched
     * item list. Prefer this overload when multiple managers reload in the same
     * cycle - it eliminates redundant {@code ItemsAdder.getAllItems()} calls.
     *
     * @param items the shared, pre-fetched list of all ItemsAdder items
     */
    public void reload(List<CustomStack> items) {
        loader.load(items);
    }

    /**
     * Returns the registry so external plugins can register custom actions
     * before the first reload.
     */
    public ExecutorRegistry<ActionExecutor> registry() {
        return registry;
    }
}
