package toutouchien.itemsadderadditions.runtime.reload;

import org.jspecify.annotations.NullMarked;

/**
 * A subsystem that rebuilds state from ItemsAdder content during a reload.
 */
@NullMarked
public interface ReloadableContentSystem {
    String name();

    ReloadPhase phase();

    ReloadStepResult reload(ContentReloadContext context);
}
