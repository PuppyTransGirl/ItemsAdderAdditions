package toutouchien.itemsadderadditions.feature.tag;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.CustomTagRegistry;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.runtime.reload.ReloadableContentSystem;

@NullMarked
public final class CustomTagReloadSystem implements ReloadableContentSystem {
    private static final String LOG_TAG = "CustomTags";

    private final CustomTagLoader loader = new CustomTagLoader();

    @Override
    public String name() {
        return "CustomTags";
    }

    @Override
    public ReloadPhase phase() {
        return ReloadPhase.REGISTRY_PREPARE;
    }

    @Override
    public ReloadStepResult reload(ContentReloadContext context) {
        CustomTagRegistry registry = loader.loadAll(context.registry());
        NamespaceUtils.setCustomTagRegistry(registry);
        Log.info(LOG_TAG,
                "Loaded {} custom tag(s) (definitions={}, invalid={}).",
                registry.tagCount(), registry.definitionCount(), registry.invalidCount());
        return ReloadStepResult.loaded(name(), registry.tagCount());
    }
}
