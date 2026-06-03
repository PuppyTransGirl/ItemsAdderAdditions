package toutouchien.itemsadderadditions.feature.action.loading;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.TriggerKey;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the fully-loaded, per-item action bindings.
 * Key: item / entity namespaced ID.
 *
 * <p>Bindings are keyed by {@link TriggerKey}, which pairs a {@link TriggerType} with
 * an optional event argument. For triggers that carry no argument (the vast majority),
 * the key's argument is {@code null}. For argumentized triggers such as
 * {@code interact}, each argument variant (e.g. {@code "right"}, {@code "left_shift"})
 * is stored and looked up independently.
 */
@NullMarked
public final class ActionBindings {
    private static final Map<String, Map<TriggerKey, List<ActionExecutor>>> bindings = new HashMap<>();

    private ActionBindings() {
        throw new IllegalStateException("Static class");
    }

    public static void clear() {
        bindings.clear();
    }

    /**
     * Adds an executor bound to a full {@link TriggerKey} (type + optional argument).
     */
    public static void add(String id, TriggerKey key, ActionExecutor executor) {
        bindings.computeIfAbsent(id, k -> new HashMap<>())
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(executor);
    }

    /**
     * Convenience overload for non-argumentized triggers.
     */
    public static void add(String id, TriggerType type, ActionExecutor executor) {
        add(id, TriggerKey.of(type), executor);
    }

    /**
     * Returns all executors bound to {@code (id, type, argument)}.
     *
     * <p>When {@code argument} is non-null (i.e. an argumentized trigger fires at runtime),
     * this method returns executors in two groups, concatenated:
     * <ol>
     *   <li>Executors registered for the exact {@code (type, argument)} key - these were
     *       declared with a specific argument sub-key in YAML (e.g. {@code right:}).</li>
     *   <li>Executors registered for the wildcard {@code (type, null)} key - these were
     *       declared without any argument sub-key in YAML, meaning "fire on any interaction".</li>
     * </ol>
     * When {@code argument} is {@code null} only the exact (null-argument) key is looked up,
     * which is the normal path for non-argumentized triggers.
     *
     * @param argument the event argument from the runtime context
     *                 ({@code null} for non-argumentized triggers)
     */
    public static List<ActionExecutor> get(String id, TriggerType type, @Nullable String argument) {
        List<ActionExecutor> result = new ArrayList<>();

        collectForKey(id, type, argument, result);

        // Fall back to the base (rotation-stripped) ID for IA block/furniture variants.
        String baseId = NamespaceUtils.stripRotationSuffix(id);
        if (!baseId.equals(id)) {
            collectForKey(baseId, type, argument, result);
        }

        // Vanilla tag bindings, e.g. #minecraft:logs or #minecraft:planks.
        for (Map.Entry<String, Map<TriggerKey, List<ActionExecutor>>> entry : bindings.entrySet()) {
            String bindingId = entry.getKey();
            if (!bindingId.startsWith("#")) continue;
            if (!matchesTagBinding(id, bindingId)) continue;
            collectFromMap(entry.getValue(), type, argument, result);
        }

        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    private static void collectForKey(
            String id,
            TriggerType type,
            @Nullable String argument,
            List<ActionExecutor> result
    ) {
        Map<TriggerKey, List<ActionExecutor>> keyMap = bindings.get(id);
        if (keyMap == null) return;
        collectFromMap(keyMap, type, argument, result);
    }

    private static void collectFromMap(
            Map<TriggerKey, List<ActionExecutor>> keyMap,
            TriggerType type,
            @Nullable String argument,
            List<ActionExecutor> result
    ) {
        result.addAll(keyMap.getOrDefault(TriggerKey.of(type, argument), List.of()));

        // When a specific argument is present, also include executors registered without
        // any argument (the wildcard case: "fire on any interaction").
        if (argument != null) {
            result.addAll(keyMap.getOrDefault(TriggerKey.of(type, null), List.of()));
        }
    }

    /**
     * Convenience overload for non-argumentized triggers.
     */
    public static List<ActionExecutor> get(String id, TriggerType type) {
        return get(id, type, null);
    }

    public static boolean has(String id, TriggerType type, @Nullable String argument) {
        return !get(id, type, argument).isEmpty();
    }

    /**
     * Convenience overload for non-argumentized triggers.
     */
    public static boolean has(String id, TriggerType type) {
        return has(id, type, null);
    }

    private static boolean matchesTagBinding(String id, String bindingId) {
        return NamespaceUtils.matchesContentIDOrTag(id, bindingId, CustomTagType.ITEM)
                || NamespaceUtils.matchesContentIDOrTag(id, bindingId, CustomTagType.BLOCK)
                || NamespaceUtils.matchesContentIDOrTag(id, bindingId, CustomTagType.FURNITURE);
    }
}
