package toutouchien.itemsadderadditions.actions;

import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.actions.executors.ActionBarAction;
import toutouchien.itemsadderadditions.actions.executors.OpenInventoryAction;
import toutouchien.itemsadderadditions.actions.executors.PlayAnimationAction;
import toutouchien.itemsadderadditions.actions.executors.SwingHandAction;
import toutouchien.itemsadderadditions.actions.executors.TeleportAction;
import toutouchien.itemsadderadditions.actions.executors.TitleAction;
import toutouchien.itemsadderadditions.actions.executors.ToastAction;
import toutouchien.itemsadderadditions.actions.executors.VeinminerAction;
import toutouchien.itemsadderadditions.actions.loading.ActionLoader;
import toutouchien.itemsadderadditions.utils.ExecutorRegistry;

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
 * {@code actions} section, e.g.:
 * <pre>{@code
 * actions:
 *   veinminer: false
 *   title:     false
 * }</pre>
 */
public final class ActionsManager {
    /** Config key prefix matching the {@code actions:} section in config.yml. */
    private static final String CONFIG_SECTION = "actions.";

    private final ExecutorRegistry<ActionExecutor> registry = new ExecutorRegistry<>("[Actions]");
    private final ActionLoader loader = new ActionLoader(registry);

    public ActionsManager() {
        registerBuiltins(
            new ActionBarAction(),
            new OpenInventoryAction(),
            new PlayAnimationAction(),
            new SwingHandAction(),
            new TeleportAction(),
            new TitleAction(),
            new ToastAction(),
            new VeinminerAction()
        );
    }

    /**
     * Registers built-in executors, skipping any whose {@code actions.<key>}
     * config entry is set to {@code false}.
     */
    private void registerBuiltins(ActionExecutor... executors) {
        for (ActionExecutor executor : executors) {
            String configPath = CONFIG_SECTION + executor.key();
            if (!ItemsAdderAdditions.instance().getConfig().getBoolean(configPath, true)) {
                ItemsAdderAdditions.instance().getSLF4JLogger().info(
                    "[Actions] Skipping '{}' - disabled in config.", executor.key()
                );
                continue;
            }

            registry.register(executor);
        }
    }

    /**
     * (Re)loads every item's actions from its YAML config.
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
