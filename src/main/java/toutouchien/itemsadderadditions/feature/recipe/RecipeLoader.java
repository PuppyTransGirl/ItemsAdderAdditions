package toutouchien.itemsadderadditions.feature.recipe;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.loading.CategorizedConfigFile;
import toutouchien.itemsadderadditions.common.loading.ConfigFileCategory;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.recipe.campfire.CampfireRecipeHandler;
import toutouchien.itemsadderadditions.feature.recipe.crafting.CraftingRecipeHandler;
import toutouchien.itemsadderadditions.feature.recipe.stonecutter.StonecutterRecipeHandler;

import java.util.List;

/**
 * Dispatches custom recipe sections from pre-filtered YAML files to the
 * appropriate recipe handlers.
 *
 * <h3>Supported YAML keys under {@code recipes:}</h3>
 * <ul>
 *   <li>{@code campfire_cooking}    → {@link CampfireRecipeHandler}</li>
 *   <li>{@code stonecutter}         → {@link StonecutterRecipeHandler}</li>
 *   <li>{@code iaa_crafting_table}  → {@link CraftingRecipeHandler} (shaped, 3×3)</li>
 *   <li>{@code iaa_crafting}        → {@link CraftingRecipeHandler} (shaped or shapeless, 2×2)</li>
 * </ul>
 *
 * <h3>Optimized flow (preferred)</h3>
 * Use {@link #loadAll(ConfigFileRegistry)} to process only files that were already
 * identified as containing recipe sections by the central scanner. Files are never
 * re-read from disk - the YAML was parsed exactly once during the registry scan.
 */
@NullMarked
public final class RecipeLoader {
    private static final String LOG_TAG = "RecipeLoader";

    /**
     * All crafting section keys dispatched to the crafting handler.
     */
    private static final List<String> CRAFTING_SECTION_KEYS = List.of(
            "iaa_crafting_table",
            "iaa_crafting"
    );

    private final CampfireRecipeHandler campfireHandler;
    private final StonecutterRecipeHandler stonecutterHandler;
    private final CraftingRecipeHandler craftingHandler;

    public RecipeLoader(
            CampfireRecipeHandler campfireHandler,
            StonecutterRecipeHandler stonecutterHandler,
            CraftingRecipeHandler craftingHandler
    ) {
        this.campfireHandler = campfireHandler;
        this.stonecutterHandler = stonecutterHandler;
        this.craftingHandler = craftingHandler;
    }

    /**
     * Loads every custom recipe type from pre-filtered files supplied by the
     * {@link ConfigFileRegistry}.
     *
     * <p>The registry provides a deduplicated union of all recipe-category files.
     * Each file is visited exactly once; the {@link CategorizedConfigFile#hasCategory}
     * flag drives which handlers are invoked for that file, so a file containing
     * both campfire and stonecutter recipes is processed correctly in a single pass.
     *
     * @param registry the registry built during the current reload cycle
     */
    public void loadAll(ConfigFileRegistry registry) {
        resetCounters();

        // Union of all recipe files, deduplicated - a file with multiple recipe
        // types appears only once and both handlers are invoked for it.
        List<CategorizedConfigFile> files = registry.getFiles(
                ConfigFileCategory.CAMPFIRE_RECIPES,
                ConfigFileCategory.STONECUTTER_RECIPES,
                ConfigFileCategory.CRAFTING_RECIPES
        );

        Log.info(LOG_TAG, "Processing {} YAML file(s) for custom recipes...", files.size());

        for (CategorizedConfigFile ccf : files) {
            loadFile(ccf);
        }

        logCounters();
    }

    /**
     * Total recipes loaded across all types in the last load call.
     */
    public int totalLoadedCount() {
        return campfireHandler.loadedCount()
                + stonecutterHandler.loadedCount()
                + craftingHandler.loadedCount();
    }

    /**
     * Processes a {@link CategorizedConfigFile} using its pre-computed category
     * flags to dispatch only the handlers that are relevant for this file.
     */
    private void loadFile(CategorizedConfigFile ccf) {
        try {
            YamlConfiguration yaml = ccf.yaml();

            ConfigurationSection info = yaml.getConfigurationSection("info");
            if (info == null) return;

            String namespace = info.getString("namespace");
            if (namespace == null || namespace.isBlank()) return;

            ConfigurationSection recipes = yaml.getConfigurationSection("recipes");
            if (recipes == null) return;

            // Dispatch only the handlers whose category is confirmed for this file.
            if (ccf.hasCategory(ConfigFileCategory.CAMPFIRE_RECIPES)) {
                campfireHandler.load(namespace, recipes.getConfigurationSection("campfire_cooking"));
            }
            if (ccf.hasCategory(ConfigFileCategory.STONECUTTER_RECIPES)) {
                stonecutterHandler.load(namespace, recipes.getConfigurationSection("stonecutter"));
            }
            if (ccf.hasCategory(ConfigFileCategory.CRAFTING_RECIPES)) {
                for (String craftingKey : CRAFTING_SECTION_KEYS) {
                    craftingHandler.load(namespace, recipes.getConfigurationSection(craftingKey));
                }
            }
        } catch (Exception e) {
            Log.error(LOG_TAG, "Failed to parse file: " + ccf.file().getPath(), e);
        }
    }

    private void resetCounters() {
        campfireHandler.resetCount();
        stonecutterHandler.resetCount();
        craftingHandler.resetCount();
    }

    private void logCounters() {
        Log.debug(LOG_TAG, "Campfire recipes loaded: {}", campfireHandler.loadedCount());
        Log.debug(LOG_TAG, "Stonecutter recipes loaded: {}", stonecutterHandler.loadedCount());
        Log.debug(LOG_TAG, "Crafting recipes loaded: {}", craftingHandler.loadedCount());
    }
}
