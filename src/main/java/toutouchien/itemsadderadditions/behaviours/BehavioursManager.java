package toutouchien.itemsadderadditions.behaviours;

import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.behaviours.executors.*;
import toutouchien.itemsadderadditions.behaviours.loading.BehaviourLoader;
import toutouchien.itemsadderadditions.utils.other.ExecutorRegistry;

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
 *   connectable:    false
 *   contact_damage: false
 *   stackable:      false
 *   storage:        false
 * }</pre>
 */
public final class BehavioursManager {
    private static final String CONFIG_PREFIX = "behaviours.";

    private final ExecutorRegistry<BehaviourExecutor> registry = new ExecutorRegistry<>("Behaviours");
    private final BehaviourLoader loader = new BehaviourLoader(registry);

    public BehavioursManager() {
        registry.registerIfEnabled(
                ItemsAdderAdditions.instance().getConfig(),
                CONFIG_PREFIX,
                new BedBehaviour(),
                new ConnectableBehaviour(),
                new ContactDamageBehaviour(),
                new StackableBehaviour(),
                new StorageBehaviour()
        );
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
