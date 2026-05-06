package toutouchien.itemsadderadditions.recipes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.recipes.campfire.CampfireRecipeHandler;
import toutouchien.itemsadderadditions.recipes.crafting.CraftingRecipeHandler;
import toutouchien.itemsadderadditions.recipes.stonecutter.StonecutterRecipeHandler;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scans every {@code .yml} file inside ItemsAdder's {@code contents/} folder
 * and dispatches recipe sections to the specialised recipe handlers.
 *
 * <h3>Supported YAML keys under {@code recipes:}</h3>
 * <ul>
 *   <li>{@code campfire_cooking}    → {@link CampfireRecipeHandler}</li>
 *   <li>{@code stonecutter}         → {@link StonecutterRecipeHandler}</li>
 *   <li>{@code iaa_crafting_table}  → {@link CraftingRecipeHandler} (shaped, 3×3)</li>
 *   <li>{@code iaa_crafting}        → {@link CraftingRecipeHandler} (shaped or shapeless, 2×2)</li>
 * </ul>
 */
@NullMarked
public final class RecipeLoader {
    private static final String LOG_TAG = "RecipeLoader";

    /**
     * All crafting section keys dispatched to the crafting handler.
     * Both shapes are parsed identically - the distinction is cosmetic for config authors.
     */
    private static final List<String> CRAFTING_SECTION_KEYS = List.of(
            "iaa_crafting_table",
            "iaa_crafting"
    );

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

    private static List<File> collectYamlFiles(Path dir) {
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream
                    .filter(path -> Files.isRegularFile(path)
                            && path.getFileName().toString().endsWith(".yml"))
                    .map(Path::toFile)
                    .toList();
        } catch (IOException e) {
            Log.error(LOG_TAG, "Failed to scan recipe directory: " + dir, e);
            return List.of();
        }
    }

    /**
     * Scans all YAML files and loads every custom recipe type.
     */
    public void loadAll() {
        if (!itemsAdderContentsDir.exists()) {
            Log.warn(LOG_TAG, "ItemsAdder contents directory not found: {}",
                    itemsAdderContentsDir.getPath());
            return;
        }

        // Reset per-type counters before each load cycle
        campfireHandler.resetCount();
        stonecutterHandler.resetCount();
        craftingHandler.resetCount();

        List<File> yamlFiles = collectYamlFiles(itemsAdderContentsDir.toPath());
        Log.info(LOG_TAG, "Scanning {} YAML file(s) for custom recipes...", yamlFiles.size());

        for (File file : yamlFiles) {
            loadFile(file);
        }

        // Per-type debug breakdown
        Log.debug(LOG_TAG, "Campfire recipes loaded:    {}", campfireHandler.loadedCount());
        Log.debug(LOG_TAG, "Stonecutter recipes loaded: {}", stonecutterHandler.loadedCount());
        Log.debug(LOG_TAG, "Crafting recipes loaded:    {}", craftingHandler.loadedCount());
    }

    /**
     * Total recipes loaded across all types in the last {@link #loadAll()} call.
     */
    public int totalLoadedCount() {
        return campfireHandler.loadedCount()
                + stonecutterHandler.loadedCount()
                + craftingHandler.loadedCount();
    }

    private void loadFile(File file) {
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            ConfigurationSection info = yaml.getConfigurationSection("info");
            if (info == null) return;

            String namespace = info.getString("namespace");
            if (namespace == null || namespace.isBlank()) return;

            ConfigurationSection recipes = yaml.getConfigurationSection("recipes");
            if (recipes == null) return;

            campfireHandler.load(namespace, recipes.getConfigurationSection("campfire_cooking"));
            stonecutterHandler.load(namespace, recipes.getConfigurationSection("stonecutter"));

            for (String craftingKey : CRAFTING_SECTION_KEYS) {
                craftingHandler.load(namespace, recipes.getConfigurationSection(craftingKey));
            }
        } catch (Exception e) {
            Log.error(LOG_TAG, "Failed to parse file: " + file.getPath(), e);
        }
    }
}
