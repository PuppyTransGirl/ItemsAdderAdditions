package toutouchien.itemsadderadditions.runtime.reload;

import org.jspecify.annotations.NullMarked;

/**
 * Summary of one ItemsAdderAdditions data reload cycle.
 */
@NullMarked
public record ReloadResult(
        boolean registryChanged,
        int filesScanned,
        int filesTagged
) {}
