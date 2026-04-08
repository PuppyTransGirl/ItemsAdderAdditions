package toutouchien.itemsadderadditions.recipes;

import org.bukkit.Bukkit;
import toutouchien.itemsadderadditions.recipes.campfire.CampfireRecipeHandler;
import toutouchien.itemsadderadditions.recipes.stonecutter.StonecutterRecipeHandler;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.io.File;

/**
 * Orchestrates loading and unloading of all custom recipe types
 * added by ItemsAdderAdditions.
 *
 * <p>Call {@link #reload()} on every {@code ItemsAdderLoadDataEvent} so that
 * recipes always stay in sync with ItemsAdder's own reload cycle.
 */
public class RecipeManager {
    private static final String LOG_TAG = "RecipeManager";

    private final CampfireRecipeHandler campfireHandler = new CampfireRecipeHandler();
    private final StonecutterRecipeHandler stonecutterHandler = new StonecutterRecipeHandler();
    private final RecipeLoader loader;

    public RecipeManager() {
        File iaDataFolder = new File(
                Bukkit.getPluginManager().getPlugin("ItemsAdder").getDataFolder(),
                "contents"
        );
        this.loader = new RecipeLoader(iaDataFolder, campfireHandler, stonecutterHandler);
    }

    /**
     * Unregisters all previously loaded recipes, then re-scans and re-registers.
     * Safe to call multiple times (e.g. on /iareload).
     */
    public void reload() {
        unregisterAll();
        Log.info(LOG_TAG, "Loading custom recipes...");
        loader.loadAll();

        // Sync recipe book for all online players
        Bukkit.getOnlinePlayers().forEach(p ->
                p.updateInventory()
        );
    }

    private void unregisterAll() {
        campfireHandler.unregisterAll();
        stonecutterHandler.unregisterAll();
    }

    public CampfireRecipeHandler campfireHandler() {
        return campfireHandler;
    }

    public StonecutterRecipeHandler stonecutterHandler() {
        return stonecutterHandler;
    }
}
