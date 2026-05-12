package toutouchien.itemsadderadditions.runtime.reload;

import dev.lone.itemsadder.api.CustomStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;

import java.util.List;

/**
 * Immutable data shared by one ItemsAdder content reload.
 *
 * <p>The expensive inputs are created once per reload cycle and passed through
 * every system that needs them. Systems should not call ItemsAdder#getAllItems()
 * or rescan the contents folder on their own.</p>
 */
@NullMarked
public record ContentReloadContext(
        List<CustomStack> items,
        ConfigFileRegistry registry
) {}
