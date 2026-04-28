package toutouchien.itemsadderadditions.recipes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import toutouchien.itemsadderadditions.recipes.campfire.CampfireRecipeHandler;
import toutouchien.itemsadderadditions.recipes.crafting.CraftingRecipeHandler;
import toutouchien.itemsadderadditions.recipes.stonecutter.StonecutterRecipeHandler;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans every {@code .yml} file inside ItemsAdder's {@code contents/} folder
 * (recursively) and dispatches the {@code recipes.campfire_cooking} and
 * {@code recipes.stonecutter} sections to their respective handlers.
 */
public class RecipeLoader {
    private static final String LOG_TAG = "RecipeLoader";

    private final File itemsAdderContentsDir;
    private final CampfireRecipeHandler campfireHandler;
    private final StonecutterRecipeHandler stonecutterHandler;
    private final CraftingRecipeHandler craftingHandler;

    public RecipeLoader(
            File itemsAdderContentsDir,
            CampfireRecipeHandler campfireHandler,
            StonecutterRecipeHandler stonecutterHandler,
            CraftingRecipeHandler craftingHandler
    ) {
        this.itemsAdderContentsDir = itemsAdderContentsDir;
        this.campfireHandler = campfireHandler;
        this.stonecutterHandler = stonecutterHandler;
        this.craftingHandler = craftingHandler;
    }

    private static List<File> collectYamlFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) return result;
        for (File f : children) {
            if (f.isDirectory())
                result.addAll(collectYamlFiles(f));
            else if (f.getName().endsWith(".yml"))
                result.add(f);
        }

        return result;
    }

    /**
     * Scans all YAML files and loads every custom recipe type.
     */
    public void loadAll() {
        if (!itemsAdderContentsDir.exists()) {
            Log.warn(LOG_TAG, "ItemsAdder contents directory not found: "
                    + itemsAdderContentsDir.getPath());
            return;
        }

        List<File> yamlFiles = collectYamlFiles(itemsAdderContentsDir);
        Log.info(LOG_TAG, "Scanning " + yamlFiles.size() + " YAML files for custom recipes...");

        for (File file : yamlFiles) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                // Resolve namespace
                ConfigurationSection info = yaml.getConfigurationSection("info");
                if (info == null) continue;

                String namespace = info.getString("namespace");
                if (namespace == null || namespace.isBlank()) continue;

                ConfigurationSection recipes = yaml.getConfigurationSection("recipes");
                if (recipes == null) continue;

                campfireHandler.load(
                        namespace,
                        recipes.getConfigurationSection("campfire_cooking")
                );

                stonecutterHandler.load(
                        namespace,
                        recipes.getConfigurationSection("stonecutter")
                );

                craftingHandler.load(
                        namespace,
                        recipes.getConfigurationSection("crafting")
                );
            } catch (Exception e) {
                Log.warn(LOG_TAG, "Failed to parse file: " + file.getPath()
                        + " - " + e.getMessage());
            }
        }
    }
}
