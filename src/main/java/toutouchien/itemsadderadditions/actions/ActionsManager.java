package toutouchien.itemsadderadditions.actions;

import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.actions.executors.*;
import toutouchien.itemsadderadditions.actions.loading.ActionLoader;
import toutouchien.itemsadderadditions.utils.other.ExecutorRegistry;
import toutouchien.itemsadderadditions.utils.other.Log;

/**
 * Entry point for the actions system.
 *
 * <p>Instantiate once during plugin enable; then call {@link #reload()} after
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
    /**
     * Config key prefix matching the {@code actions:} section in config.yml.
     */
    private static final String CONFIG_SECTION = "actions.";

    private final ExecutorRegistry<ActionExecutor> registry = new ExecutorRegistry<>("Actions");
    private final ActionLoader loader = new ActionLoader(registry);

    public ActionsManager() {
        registerBuiltins(
                new ActionBarAction(),
                new ClearItemAction(),
                new IgniteAction(),
                new MessageAction(),
                new MythicMobsSkillAction(),
                new OpenInventoryAction(),
                new PlayAnimationAction(),
                new PlayEmoteAction(),
                new ShootFireballAction(),
                new SwingHandAction(),
                new TeleportAction(),
                new TitleAction(),
                new ToastAction(),
                new VeinminerAction()
        );
    }

    /**
     * Registers built-in executors, skipping any whose {@code actions.<key>}
     * config entry is explicitly set to {@code false}.
     */
    private void registerBuiltins(ActionExecutor... executors) {
        for (ActionExecutor executor : executors) {
            String configPath = CONFIG_SECTION + executor.key();
            if (!ItemsAdderAdditions.instance().getConfig().getBoolean(configPath, true)) {
                Log.disabled("Actions", executor.key());
                continue;
            }
            registry.register(executor);
        }
    }

    /**
     * (Re)loads every item's actions from its YAML config.
     * Safe to call multiple times.
     */
    public void reload() {
        loader.load();
    }

    /**
     * Returns the registry so external plugins can register custom actions
     * before the first reload.
     */
    public ExecutorRegistry<ActionExecutor> registry() {
        return registry;
    }
}
