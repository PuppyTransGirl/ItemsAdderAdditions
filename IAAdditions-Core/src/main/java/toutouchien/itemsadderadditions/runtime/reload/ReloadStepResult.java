package toutouchien.itemsadderadditions.runtime.reload;

import org.jspecify.annotations.NullMarked;

/**
 * Result produced by one reloadable system.
 */
@NullMarked
public record ReloadStepResult(
        String system,
        boolean registryChanged,
        int loadedCount
) {
    public static ReloadStepResult unchanged(String system) {
        return new ReloadStepResult(system, false, 0);
    }

    public static ReloadStepResult loaded(String system, int loadedCount) {
        return new ReloadStepResult(system, false, loadedCount);
    }

    public static ReloadStepResult registry(String system, boolean registryChanged, int loadedCount) {
        return new ReloadStepResult(system, registryChanged, loadedCount);
    }
}
