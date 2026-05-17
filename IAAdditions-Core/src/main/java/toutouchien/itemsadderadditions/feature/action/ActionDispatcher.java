package toutouchien.itemsadderadditions.feature.action;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.action.loading.ActionBindings;

import java.util.List;

/**
 * Small runtime gateway between Bukkit event listeners and loaded action bindings.
 *
 * <p>Listeners only translate events into {@link ActionContext}s. This class owns
 * lookup, debug logging, and executor invocation so dispatch behavior stays
 * consistent across every trigger.</p>
 */
@NullMarked
public final class ActionDispatcher {
    private static final String LOG_TAG = "Dispatch";

    public void dispatch(String id, TriggerType type, ActionContext context) {
        dispatch(id, type, null, context);
    }

    public void dispatch(String id, TriggerType type, @Nullable String argument, ActionContext context) {
        List<ActionExecutor> executors = ActionBindings.get(id, type, argument);

        Log.debug(LOG_TAG,
                "Lookup id={}, type={}, argument={} -> {} executor(s)",
                id, type, argument, executors.size());

        if (executors.isEmpty()) {
            return;
        }

        for (ActionExecutor executor : executors) {
            Log.debug(LOG_TAG, "Running executor {}", executor.getClass().getSimpleName());
            executor.run(context);
        }
    }
}
