package toutouchien.itemsadderadditions.recipes;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.recipes.campfire.CampfireRecipeHandler;
import toutouchien.itemsadderadditions.recipes.crafting.CraftingRecipeHandler;
import toutouchien.itemsadderadditions.recipes.crafting.CraftingRecipeListener;
import toutouchien.itemsadderadditions.recipes.stonecutter.StonecutterRecipeHandler;
import toutouchien.itemsadderadditions.utils.loading.ConfigFileRegistry;
import toutouchien.itemsadderadditions.utils.other.Log;

public final class RecipeManager {
    private static final String LOG_TAG = "RecipeManager";

    private final CampfireRecipeHandler campfireHandler = new CampfireRecipeHandler();
    private final StonecutterRecipeHandler stonecutterHandler = new StonecutterRecipeHandler();
    private final CraftingRecipeHandler craftingHandler =
            new CraftingRecipeHandler(NmsManager.instance().handler().craftingRecipes());
    private final RecipeLoader loader;

    public RecipeManager(Plugin plugin) {
        this.loader = new RecipeLoader(
                campfireHandler, stonecutterHandler, craftingHandler
        );

        Bukkit.getPluginManager().registerEvents(
                new CraftingRecipeListener(craftingHandler, plugin), plugin);
    }

    /**
     * Reloads all custom recipes using pre-filtered files from the central registry.
     * This is the preferred overload — no directory scan or YAML parsing occurs here.
     *
     * @param registry the registry built during the current reload cycle
     */
    public void reload(ConfigFileRegistry registry) {
        unregisterAll();

        Log.info(LOG_TAG, "Loading custom recipes...");
        long startMs = System.currentTimeMillis();
        try {
            loader.loadAll(registry);
        } finally {
            NmsManager.instance().handler().finalizeRecipes();
        }
        long elapsedMs = System.currentTimeMillis() - startMs;

        int total = loader.totalLoadedCount();
        Log.info(LOG_TAG,
                "Loaded {} recipe(s) in {}ms (campfire={}, stonecutter={}, crafting={}).",
                total, elapsedMs,
                campfireHandler.loadedCount(),
                stonecutterHandler.loadedCount(),
                craftingHandler.loadedCount());
        Log.debug(LOG_TAG, "Recipe finalization complete.");
    }

    private void unregisterAll() {
        campfireHandler.unregisterAll();
        stonecutterHandler.unregisterAll();
        craftingHandler.unregisterAll();

        NmsManager.instance().handler().finalizeRecipes();
    }
}
