package toutouchien.itemsadderadditions.feature.behaviour;

import dev.lone.itemsadder.api.CustomStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.registry.ExecutorRegistry;
import toutouchien.itemsadderadditions.feature.behaviour.loading.BehaviourBindings;
import toutouchien.itemsadderadditions.feature.behaviour.loading.BehaviourLoader;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.runtime.reload.ReloadableContentSystem;
import toutouchien.itemsadderadditions.settings.PluginSettings;

import java.util.List;

/**
 * Runtime facade for persistent item behaviours.
 */
@NullMarked
public final class BehavioursManager implements ReloadableContentSystem {
    private final ExecutorRegistry<BehaviourExecutor> registry = new ExecutorRegistry<>("Behaviours");
    private final BehaviourLoader loader = new BehaviourLoader(registry);

    public BehavioursManager(PluginSettings settings) {
        applySettings(settings);
    }

    public void applySettings(PluginSettings settings) {
        registry.registerBuiltIns(settings::behaviourEnabled, BuiltInBehaviours.create());
    }

    public int reload(List<CustomStack> items) {
        return loader.load(items);
    }

    @Override
    public String name() {
        return "Behaviours";
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
        BehaviourBindings.clear();
    }

    public ExecutorRegistry<BehaviourExecutor> registry() {
        return registry;
    }
}
