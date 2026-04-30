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

public class RecipeManager {
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
        loader.loadAll();

        // Single finalization after every recipe type has been registered.
        NmsManager.instance().handler().finalizeRecipes();
        Log.info(LOG_TAG, "Recipe finalization complete.");
    }

    private void unregisterAll() {
        campfireHandler.unregisterAll();
        stonecutterHandler.unregisterAll();
        craftingHandler.unregisterAll();

        // Single finalization after every recipe type has been unregistered.
        NmsManager.instance().handler().finalizeRecipes();
    }
}
