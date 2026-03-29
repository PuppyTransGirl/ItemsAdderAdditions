package toutouchien.itemsadderadditions.utils.other;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A generic, ordered map of executor prototypes keyed by their YAML config key.
 *
 * <p>The registry holds one prototype per key. Loaders call
 * {@link #getPrototype(String)} and then invoke {@code newInstance()} on the
 * prototype so each loaded item gets its own isolated, injectable copy.
 *
 * <h3>Usage</h3>
 * <pre>
 * // Create one instance per subsystem (actions, behaviours, …)
 * ExecutorRegistry&lt;ActionExecutor&gt; registry = new ExecutorRegistry&lt;&gt;("[Actions]");
 * registry.register(new TitleAction(), new ActionBarAction());
 *
 * // From a loader
 * ActionExecutor proto = registry.getPrototype("title"); // non-null if registered
 * </pre>
 *
 * <h3>External plugin registration</h3>
 * Retrieve the registry from the relevant manager and call
 * {@link #register(Keyed[])} before the first reload:
 * <pre>
 * ItemsAdderAdditions.instance().actionsManager().registry().register(new MyCustomAction());
 * </pre>
 *
 * @param <E> the executor type; must implement {@link Keyed} so the registry can
 *            read the key without knowing the concrete annotation class
 */
@NullMarked
public final class ExecutorRegistry<E extends Keyed> {
    private final Map<String, E> prototypes = new LinkedHashMap<>();
    private final String logPrefix;

    /**
     * @param logPrefix short prefix included in log messages, e.g. {@code "Actions"}
     */
    public ExecutorRegistry(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    /**
     * Registers one or more executor prototypes.
     *
     * <p>If an executor's {@link Keyed#key()} throws (meaning the required
     * class-level annotation is absent), registration of that executor is skipped
     * with a warning and the remaining executors continue to be processed.
     *
     * @param executors the prototypes to register
     */
    @SafeVarargs
    public final void register(E... executors) {
        for (E e : executors) {
            String key;
            try {
                key = e.key();
            } catch (IllegalStateException ex) {
                Log.warn(logPrefix, "Skipping registration - missing @{} annotation on {}",
                        "Behaviour/Action", e.getClass().getName());
                continue;
            }
            prototypes.put(key, e);
            Log.registered(logPrefix, key);
        }
    }

    /**
     * Returns the prototype registered under {@code key}, or {@code null} if none.
     */
    @Nullable
    public E getPrototype(String key) {
        return prototypes.get(key);
    }

    /**
     * Returns an unmodifiable view of all registered prototypes.
     */
    public Collection<E> getAll() {
        return Collections.unmodifiableCollection(prototypes.values());
    }
}
