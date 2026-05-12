package toutouchien.itemsadderadditions.runtime.reload;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Runs reloadable systems in a deterministic phase order.
 */
@NullMarked
public final class ReloadPlan {
    private static final String LOG_TAG = "Reload";

    private final List<ReloadableContentSystem> systems;

    public ReloadPlan(List<ReloadableContentSystem> systems) {
        this.systems = systems.stream()
                .sorted(Comparator.comparing(ReloadableContentSystem::phase))
                .toList();
    }

    public List<ReloadStepResult> run(ContentReloadContext context) {
        List<ReloadStepResult> results = new ArrayList<>();
        for (ReloadableContentSystem system : systems) {
            long start = System.currentTimeMillis();
            ReloadStepResult result = system.reload(context);
            results.add(result);
            Log.debug(LOG_TAG, "{} reloaded in {}ms (loaded={}, registryChanged={}).",
                    system.name(), System.currentTimeMillis() - start,
                    result.loadedCount(), result.registryChanged());
        }
        return List.copyOf(results);
    }
}
