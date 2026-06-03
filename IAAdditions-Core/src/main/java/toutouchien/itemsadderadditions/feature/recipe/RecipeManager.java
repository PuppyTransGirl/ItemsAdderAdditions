package toutouchien.itemsadderadditions.feature.recipe;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.recipe.brewing.BrewingRecipeHandler;
import toutouchien.itemsadderadditions.feature.recipe.brewing.BrewingRecipeListener;
import toutouchien.itemsadderadditions.feature.recipe.campfire.CampfireRecipeHandler;
import toutouchien.itemsadderadditions.feature.recipe.crafting.CraftingRecipeHandler;
import toutouchien.itemsadderadditions.feature.recipe.crafting.CraftingRecipeListener;
import toutouchien.itemsadderadditions.feature.recipe.stonecutter.StonecutterRecipeHandler;
import toutouchien.itemsadderadditions.feature.recipe.stonecutter.StonecutterRecipeListener;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.runtime.reload.ContentReloadContext;
import toutouchien.itemsadderadditions.runtime.reload.ReloadPhase;
import toutouchien.itemsadderadditions.runtime.reload.ReloadStepResult;
import toutouchien.itemsadderadditions.runtime.reload.ReloadableContentSystem;

@NullMarked
public final class RecipeManager implements ReloadableContentSystem {
    private static final String LOG_TAG = "RecipeManager";

    private final CampfireRecipeHandler campfireHandler = new CampfireRecipeHandler();
    private final StonecutterRecipeHandler stonecutterHandler = new StonecutterRecipeHandler();
    private final BrewingRecipeHandler brewingHandler = new BrewingRecipeHandler();
    private final CraftingRecipeHandler craftingHandler =
            new CraftingRecipeHandler(NmsManager.instance().handler().craftingRecipes());
    private final BrewingRecipeListener brewingListener;
    private final RecipeLoader loader;

    public RecipeManager(Plugin plugin) {
        this.brewingListener = new BrewingRecipeListener(brewingHandler);
        this.loader = new RecipeLoader(campfireHandler, stonecutterHandler, brewingHandler, craftingHandler);
        Bukkit.getPluginManager().registerEvents(new CraftingRecipeListener(craftingHandler, plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new StonecutterRecipeListener(stonecutterHandler), plugin);
        Bukkit.getPluginManager().registerEvents(brewingListener, plugin);
    }

    public int reload(ConfigFileRegistry registry) {
        unregisterAll();

        Log.info(LOG_TAG, "Loading custom recipes...");
        long startMs = System.currentTimeMillis();
        try {
            loader.loadAll(registry);
        } finally {
            NmsManager.instance().handler().finalizeRecipes();
        }

        int total = loader.totalLoadedCount();
        Log.info(LOG_TAG,
                "Loaded {} recipe(s) in {}ms (campfire={}, stonecutter={}, brewing={}, crafting={}).",
                total, System.currentTimeMillis() - startMs,
                campfireHandler.loadedCount(),
                stonecutterHandler.loadedCount(),
                brewingHandler.loadedCount(),
                craftingHandler.loadedCount());
        Log.debug(LOG_TAG, "Recipe finalization complete.");
        return total;
    }

    @Override
    public String name() {
        return "Recipes";
    }

    @Override
    public ReloadPhase phase() {
        return ReloadPhase.CONTENT_FILES;
    }

    @Override
    public ReloadStepResult reload(ContentReloadContext context) {
        return ReloadStepResult.loaded(name(), reload(context.registry()));
    }

    public void shutdown() {
        unregisterAll();
    }

    private void unregisterAll() {
        brewingListener.clear();
        campfireHandler.unregisterAll();
        stonecutterHandler.unregisterAll();
        brewingHandler.unregisterAll();
        craftingHandler.unregisterAll();
        NmsManager.instance().handler().finalizeRecipes();
    }
}
