package toutouchien.itemsadderadditions.behaviours;

import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.behaviours.executors.ConnectableBehaviour;
import toutouchien.itemsadderadditions.behaviours.executors.ContactDamageBehaviour;
import toutouchien.itemsadderadditions.behaviours.executors.StackableBehaviour;
import toutouchien.itemsadderadditions.behaviours.loading.BehaviourLoader;
import toutouchien.itemsadderadditions.utils.ExecutorRegistry;

/**
 * Entry point for the behaviours system.
 *
 * <p>Instantiate once during plugin enable; then call {@link #reload()} after
 * ItemsAdder fires {@code ItemsAdderLoadDataEvent} so all custom items are available.
 *
 * <h3>Registering custom behaviours</h3>
 * Call {@link #registry()}{@code .register(...)} before the first reload,
 * either here in the constructor (for built-in behaviours) or from an external plugin's
 * {@code onEnable}.
 *
 * <h3>Disabling built-in behaviours</h3>
 * Each built-in behaviour can be disabled in {@code config.yml} under the
 * {@code behaviours} section, e.g.:
 * <pre>{@code
 * behaviours:
 *   stackable:    false
 *   contact_damage: false
 * }</pre>
 */
public final class BehavioursManager {
    /**
     * Config key prefix matching the {@code behaviours:} section in config.yml.
     */
    private static final String CONFIG_SECTION = "behaviours.";

    private final ExecutorRegistry<BehaviourExecutor> registry = new ExecutorRegistry<>("[Behaviours]");
    private final BehaviourLoader loader = new BehaviourLoader(registry);

    public BehavioursManager() {
        registerBuiltins(
                new ConnectableBehaviour(),
                new ContactDamageBehaviour(),
                new StackableBehaviour()
        );
    }

    /**
     * Registers built-in executors, skipping any whose {@code behaviours.<key>}
     * config entry is set to {@code false}.
     */
    private void registerBuiltins(BehaviourExecutor... executors) {
        for (BehaviourExecutor executor : executors) {
            String configPath = CONFIG_SECTION + executor.key();
            if (!ItemsAdderAdditions.instance().getConfig().getBoolean(configPath, true)) {
                ItemsAdderAdditions.instance().getSLF4JLogger().info(
                        "[Behaviours] Skipping '{}' - disabled in config.", executor.key()
                );
                continue;
            }

            registry.register(executor);
        }
    }

    /**
     * (Re)loads every item's behaviours from its YAML config.
     * Safe to call multiple times - active executors are unloaded before new ones are created.
     */
    public void reload() {
        loader.load();
    }

    /**
     * Returns the registry so external plugins can register custom behaviours
     * before the first reload.
     */
    public ExecutorRegistry<BehaviourExecutor> registry() {
        return registry;
    }
}
