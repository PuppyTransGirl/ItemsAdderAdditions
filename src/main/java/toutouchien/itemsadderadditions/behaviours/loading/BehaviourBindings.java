package toutouchien.itemsadderadditions.behaviours.loading;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the fully-loaded, per-item behaviour executors.
 *
 * <p>Key: item namespaced ID.<br>
 * Value: every active {@link BehaviourExecutor} instance for that item.
 *
 * <p>{@link #clear()} is the authoritative teardown point: it calls
 * {@link BehaviourExecutor#unload()} on every active executor before wiping the
 * map, ensuring listeners are unregistered and tasks cancelled even across reloads.
 */
@NullMarked
public final class BehaviourBindings {
    private static final Map<String, List<BehaviourExecutor>> bindings = new HashMap<>();

    private BehaviourBindings() {
        throw new IllegalStateException("Static class");
    }

    /**
     * Unloads all active executors, then clears the bindings map.
     * Must be called before each reload cycle begins.
     */
    public static void clear() {
        for (List<BehaviourExecutor> executors : bindings.values())
            for (BehaviourExecutor executor : executors)
                executor.unload();

        bindings.clear();
    }

    /**
     * Registers an already-loaded executor for the given item ID.
     */
    public static void add(String id, BehaviourExecutor executor) {
        bindings.computeIfAbsent(id, k -> new ArrayList<>()).add(executor);
    }

    /**
     * Returns all active behaviour executors for the given item ID.
     */
    public static List<BehaviourExecutor> get(String id) {
        return bindings.getOrDefault(id, List.of());
    }

    /**
     * Returns {@code true} if at least one behaviour is bound to the given item ID.
     */
    public static boolean has(String id) {
        return !get(id).isEmpty();
    }
}
