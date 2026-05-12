package toutouchien.itemsadderadditions.feature.worldgen;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.loading.ConfigFileCategory;
import toutouchien.itemsadderadditions.feature.worldgen.populator.FurniturePopulatorLoader;
import toutouchien.itemsadderadditions.feature.worldgen.surface.FurnitureSurfaceDecoratorLoader;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.runtime.reload.ReloadableContentSystem;

/**
 * Reloads all world-generation behaviours declared in ItemsAdder content files.
 */
@NullMarked
public final class WorldgenReloadSystem implements ReloadableContentSystem {
    @Override
    public String name() {
        return "Worldgen";
    }

    @Override
    public ReloadPhase phase() {
        return ReloadPhase.POST_CONTENT;
    }

    @Override
    public ReloadStepResult reload(ContentReloadContext context) {
        FurniturePopulatorLoader.clear();
        FurnitureSurfaceDecoratorLoader.clear();

        new FurniturePopulatorLoader()
                .loadAll(context.registry().getFiles(ConfigFileCategory.FURNITURE_POPULATORS));
        new FurnitureSurfaceDecoratorLoader()
                .loadAll(context.registry().getFiles(ConfigFileCategory.SURFACE_DECORATORS));

        return ReloadStepResult.unchanged(name());
    }
}
