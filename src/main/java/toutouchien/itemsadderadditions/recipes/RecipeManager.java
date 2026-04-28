// RecipeManager.java

package toutouchien.itemsadderadditions.recipes;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import toutouchien.itemsadderadditions.recipes.campfire.CampfireRecipeHandler;
import toutouchien.itemsadderadditions.recipes.crafting.CraftingRecipeHandler;
import toutouchien.itemsadderadditions.recipes.crafting.CraftingRecipeListener;
import toutouchien.itemsadderadditions.recipes.stonecutter.StonecutterRecipeHandler;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.io.File;

public class RecipeManager {
    private static final String LOG_TAG = "RecipeManager";

    private final CampfireRecipeHandler campfireHandler = new CampfireRecipeHandler();
    private final StonecutterRecipeHandler stonecutterHandler =
            new StonecutterRecipeHandler();
    private final CraftingRecipeHandler craftingHandler = new CraftingRecipeHandler();
    private final RecipeLoader loader;

    public RecipeManager(Plugin plugin) {
        File iaDataFolder = new File(
                Bukkit.getPluginManager().getPlugin("ItemsAdder").getDataFolder(),
                "contents");
        this.loader = new RecipeLoader(
                iaDataFolder, campfireHandler, stonecutterHandler, craftingHandler);

        // Register the crafting event listener once
        Bukkit.getPluginManager().registerEvents(
                new CraftingRecipeListener(craftingHandler, plugin), plugin);
    }

    public void reload() {
        unregisterAll();
        Log.info(LOG_TAG, "Loading custom recipes...");
        loader.loadAll();
    }

    private void unregisterAll() {
        campfireHandler.unregisterAll();
        stonecutterHandler.unregisterAll();
        craftingHandler.unregisterAll();
    }
}
