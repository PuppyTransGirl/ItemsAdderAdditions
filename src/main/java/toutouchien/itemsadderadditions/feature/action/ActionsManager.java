package toutouchien.itemsadderadditions.feature.action;

import dev.lone.itemsadder.api.CustomStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.registry.ExecutorRegistry;
import toutouchien.itemsadderadditions.feature.action.loading.ActionBindings;
import toutouchien.itemsadderadditions.feature.action.loading.ActionLoader;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.runtime.reload.ReloadableContentSystem;
import toutouchien.itemsadderadditions.settings.PluginSettings;

import java.util.List;

/**
 * Runtime facade for the action system.
 *
 * <p>The manager owns only three responsibilities: prototype registration,
 * config-driven enable/disable of built-ins, and item binding reloads. Event
 * listeners dispatch through {@link ActionBindings}; they do not parse config.</p>
 */
@NullMarked
public final class ActionsManager implements ReloadableContentSystem {
    private final ExecutorRegistry<ActionExecutor> registry = new ExecutorRegistry<>("Actions");
    private final ActionLoader loader = new ActionLoader(registry);

    public ActionsManager(PluginSettings settings) {
        applySettings(settings);
    }

    public void applySettings(PluginSettings settings) {
        registry.registerBuiltIns(settings::actionEnabled, BuiltInActions.create());
    }

    public int reload(List<CustomStack> items) {
        return loader.load(items);
    }

    @Override
    public String name() {
        return "Actions";
    }

    @Override
    public ReloadPhase phase() {
        return ReloadPhase.ITEM_BINDINGS;
    }

    @Override
    public ReloadStepResult reload(ContentReloadContext context) {
        return ReloadStepResult.loaded(name(), reload(context.items()));
    }

    public void shutdown() {
        ActionBindings.clear();
    }

    public ExecutorRegistry<ActionExecutor> registry() {
        return registry;
    }
}
