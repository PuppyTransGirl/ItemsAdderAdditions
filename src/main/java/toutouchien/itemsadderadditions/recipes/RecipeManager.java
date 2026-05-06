package toutouchien.itemsadderadditions.recipes;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.recipes.campfire.CampfireRecipeHandler;
import toutouchien.itemsadderadditions.recipes.crafting.CraftingRecipeHandler;
import toutouchien.itemsadderadditions.recipes.crafting.CraftingRecipeListener;
import toutouchien.itemsadderadditions.recipes.stonecutter.StonecutterRecipeHandler;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.io.File;

public final class RecipeManager {
    private static final String LOG_TAG = "RecipeManager";

    private final CampfireRecipeHandler campfireHandler = new CampfireRecipeHandler();
    private final StonecutterRecipeHandler stonecutterHandler = new StonecutterRecipeHandler();
    private final CraftingRecipeHandler craftingHandler =
            new CraftingRecipeHandler(NmsManager.instance().handler().craftingRecipes());
    private final RecipeLoader loader;

    public RecipeManager(Plugin plugin) {
        File iaDataFolder = new File(
                Bukkit.getPluginManager().getPlugin("ItemsAdder").getDataFolder(),
                "contents"
        );

        this.loader = new RecipeLoader(
                iaDataFolder, campfireHandler, stonecutterHandler, craftingHandler);

        Bukkit.getPluginManager().registerEvents(
                new CraftingRecipeListener(craftingHandler, plugin), plugin);
    }

    public void reload() {
        unregisterAll();

        Log.info(LOG_TAG, "Loading custom recipes...");
        long startMs = System.currentTimeMillis();
        try {
            loader.loadAll();
        } finally {
            // Keep vanilla / NMS recipe state aligned even if one file fails.
            NmsManager.instance().handler().finalizeRecipes();
        }
        long elapsedMs = System.currentTimeMillis() - startMs;

        int total = loader.totalLoadedCount();
        Log.info(LOG_TAG,
                "Loaded {} recipe(s) in {}ms  (campfire={}, stonecutter={}, crafting={}).",
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
