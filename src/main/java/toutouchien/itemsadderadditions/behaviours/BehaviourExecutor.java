package toutouchien.itemsadderadditions.behaviours;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.utils.other.Keyed;
import toutouchien.itemsadderadditions.utils.other.ParameterInjector;

/**
 * Base class for all custom item behaviours.
 *
 * <h2>What is a behaviour?</h2>
 * A behaviour is a <em>persistent mechanic</em> attached to an item type.
 * Unlike actions (which fire once per event), a behaviour is active for as
 * long as the item exists in the world. It self-manages its entire lifecycle:
 * registering Bukkit listeners, starting scheduler tasks, tracking per-player
 * state, and cleaning everything up on reload or shutdown.
 *
 * <h2>Implementing a behaviour</h2>
 * <ol>
 *   <li>Annotate the subclass with {@link Behaviour @Behaviour(key = "your_key")}.</li>
 *   <li>Declare a no-arg constructor (used by {@link #newInstance()}).</li>
 *   <li>Add {@link Parameter @Parameter}-annotated
 *       fields for any YAML parameters the behaviour needs.</li>
 *   <li>Implement {@link #onLoad}: register listeners ({@code Bukkit.getPluginManager()
 *       .registerEvents(this, host.plugin())}), start tasks, etc.</li>
 *   <li>Implement {@link #onUnload}: call {@code HandlerList.unregisterAll(this)},
 *       cancel tasks, clear player state maps.</li>
 * </ol>
 *
 * <h2>YAML structure</h2>
 * <pre>
 * behaviours:
 *   your_key:          # matches @Behaviour(key = …)
 *     some_param: 42   # injected into @Parameter fields
 * </pre>
 *
 * <h2>Lifecycle</h2>
 * <pre>
 * ItemsAdderLoadDataEvent
 *   -> BehaviourLoader.load()
 *       -> BehaviourBindings.clear()   [calls unload() on every active executor]
 *       -> for each item: inject parameters -> load(host)   [calls onLoad]
 * </pre>
 */
@NullMarked
public abstract class BehaviourExecutor implements Keyed {
    @Nullable
    private BehaviourHost host;

    /**
     * The YAML key that identifies this behaviour (from {@link Behaviour @Behaviour}).
     */
    public final String key() {
        return annotation().key();
    }

    /**
     * Creates a fresh instance of this executor via no-arg reflection.
     * Used during loading so each item gets its own injected, stateful copy.
     */
    public final BehaviourExecutor newInstance() {
        try {
            return getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "BehaviourExecutor subclass must expose a no-arg constructor: " + getClass().getName(), e
            );
        }
    }

    /**
     * Stores the host and calls {@link #onLoad}.
     * Invoked by the loader after parameter injection.
     */
    public final void load(BehaviourHost host) {
        this.host = host;
        onLoad(host);
    }

    /**
     * Calls {@link #onUnload} and nulls the host.
     * Invoked by {@link toutouchien.itemsadderadditions.behaviours.loading.BehaviourBindings#clear()}
     * before every reload.
     */
    public final void unload() {
        if (host != null) {
            onUnload(host);
            host = null;
        }
    }

    /**
     * Returns the host this executor was loaded with, or {@code null} if not yet
     * loaded (or already unloaded). Useful for scheduled tasks that need to reference
     * the item's namespace after construction.
     */
    @Nullable
    public final BehaviourHost host() {
        return host;
    }

    /**
     * Handles the transition from YAML to Java fields.
     * Can be overridden for complex behaviours (like lists or polymorphic configs).
     */
    public boolean configure(Object configData, String namespacedID) {
        // Default: if it's a section, use the standard injector.
        if (configData instanceof ConfigurationSection section)
            return ParameterInjector.inject(this, section, namespacedID);

        // If it's not a section (e.g. a List), the default injector cannot handle it.
        // Subclasses must override configure() to handle Lists, Strings, or other types.
        return false;
    }

    /**
     * Called once after {@link Parameter}
     * fields have been injected.
     *
     * @param host the item's identity and plugin reference
     */
    protected abstract void onLoad(BehaviourHost host);

    /**
     * Called before this executor is discarded (reload or shutdown).
     *
     * @param host the same host that was passed to {@link #onLoad}
     */
    protected abstract void onUnload(BehaviourHost host);

    private Behaviour annotation() {
        Behaviour b = getClass().getAnnotation(Behaviour.class);
        if (b == null)
            throw new IllegalStateException("Missing @Behaviour annotation on: " + getClass().getName());

        return b;
    }
}
