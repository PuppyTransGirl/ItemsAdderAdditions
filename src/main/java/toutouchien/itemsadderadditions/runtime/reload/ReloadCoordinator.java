package toutouchien.itemsadderadditions.runtime.reload;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.action.ActionsManager;
import toutouchien.itemsadderadditions.feature.behaviour.BehavioursManager;
import toutouchien.itemsadderadditions.feature.creative.CreativeRegistryReloader;
import toutouchien.itemsadderadditions.feature.painting.CustomPaintingManager;
import toutouchien.itemsadderadditions.feature.recipe.RecipeManager;
import toutouchien.itemsadderadditions.feature.worldgen.WorldgenReloadSystem;

import java.io.File;
import java.util.List;

/**
 * Runs one complete ItemsAdder data reload in a predictable order.
 *
 * <p>The coordinator builds the expensive reload inputs once, then delegates to
 * small reloadable systems. Commands and ItemsAdder events both use this path,
 * so all runtime data follows the same lifecycle.</p>
 */
@NullMarked
public final class ReloadCoordinator {
    private static final String LOG_TAG = "Reload";

    private final ReloadPlan reloadPlan;

    public ReloadCoordinator(
            ActionsManager actions,
            BehavioursManager behaviours,
            CustomPaintingManager paintings,
            RecipeManager recipes,
            CreativeRegistryReloader creativeRegistry
    ) {
        this.reloadPlan = new ReloadPlan(List.of(
                paintings,
                actions,
                behaviours,
                creativeRegistry,
                recipes,
                new WorldgenReloadSystem()
        ));
    }

    private static ContentReloadContext buildContext() {
        List<CustomStack> items = ItemsAdder.getAllItems();
        ConfigFileRegistry registry = ConfigFileRegistry.scan(itemsAdderContentsFolder());
        NamespaceUtils.buildCache(items);
        return new ContentReloadContext(items, registry);
    }

    private static File itemsAdderContentsFolder() {
        return new File(
                Bukkit.getPluginManager().getPlugin("ItemsAdder").getDataFolder(),
                "contents"
        );
    }

    public ReloadResult reloadItemsAdderData() {
        ContentReloadContext context = buildContext();
        List<ReloadStepResult> results = reloadPlan.run(context);
        boolean registryChanged = results.stream().anyMatch(ReloadStepResult::registryChanged);

        Log.success(LOG_TAG, "Reload complete. (items={}, files scanned={}, tagged={}, registryChanged={})",
                context.items().size(),
                context.registry().totalFilesScanned(),
                context.registry().totalFilesTagged(),
                registryChanged);

        return new ReloadResult(
                registryChanged,
                context.registry().totalFilesScanned(),
                context.registry().totalFilesTagged()
        );
    }
}
