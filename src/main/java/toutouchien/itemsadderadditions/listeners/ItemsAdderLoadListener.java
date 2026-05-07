package toutouchien.itemsadderadditions.listeners;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.creative.CreativeMenuManager;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.loading.ConfigFileCategory;
import toutouchien.itemsadderadditions.utils.loading.ConfigFileRegistry;
import toutouchien.itemsadderadditions.utils.other.Log;
import toutouchien.itemsadderadditions.worldgen.FurniturePopulatorLoader;
import toutouchien.itemsadderadditions.worldgen.FurnitureSurfaceDecoratorLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Triggers the full reload cycle whenever ItemsAdder finishes loading its own data.
 *
 * <h3>Optimized reload order</h3>
 * <ol>
 *   <li><strong>Cache items once</strong> — {@link ItemsAdder#getAllItems()} is called
 *       exactly once and the resulting list is shared with every item-based system
 *       (actions, behaviours, creative menu). This eliminates N−1 redundant API
 *       calls where N is the number of item loaders.</li>
 *   <li><strong>Scan files once</strong> — {@link ConfigFileRegistry#scan} walks the
 *       {@code contents/} directory exactly once, parses every {@code .yml} file exactly
 *       once, and categorizes them. File-based systems (recipes, world-gen) receive
 *       pre-filtered lists with no additional I/O.</li>
 *   <li>Rebuild the {@link NamespaceUtils} cache.</li>
 *   <li>Clear world-gen state from the previous cycle.</li>
 *   <li>Reload item-based systems (actions, behaviours) with the shared item list.</li>
 *   <li>Reload the creative inventory integration, if supported.</li>
 *   <li>Reload recipes with the pre-filtered recipe files.</li>
 *   <li>Register world-gen populators and surface decorators with their pre-filtered files.</li>
 * </ol>
 *
 * <h3>Adding a new file-based system</h3>
 * <ol>
 *   <li>Add a constant to {@link ConfigFileCategory} with the detection predicate.</li>
 *   <li>Create your loader and call
 *       {@code registry.getFiles(ConfigFileCategory.YOUR_CATEGORY)} to receive
 *       only the relevant, already-parsed files.</li>
 *   <li>Wire the call below in {@link #onItemsAdderLoad}.</li>
 * </ol>
 */
public final class ItemsAdderLoadListener implements Listener {
    private static void reloadCreativeMenuIfEnabled(
            ItemsAdderAdditions plugin, List<CustomStack> items) {
        boolean featureEnabled = plugin.getConfig()
                .getBoolean("features.creative_inventory_integration", true);
        if (!featureEnabled) return;

        NmsManager nms = NmsManager.instance();
        if (nms.handler().creativeMenu() == null) return;

        List<CustomStack> nonHiddenItems = new ArrayList<>();
        for (CustomStack item : items) {
            if (!shouldSkip(item)) nonHiddenItems.add(item);
        }

        nms.handler().creativeMenu().injectPaintingVariants(nonHiddenItems);
        nms.handler().creativeMenu().updatePaintingCache(nonHiddenItems);

        CreativeMenuManager creativeMenuManager = plugin.creativeMenuManager();
        if (creativeMenuManager != null) {
            creativeMenuManager.reload();
        }
    }

    private static boolean shouldSkip(CustomStack item) {
        FileConfiguration config = item.getConfig();
        return config.getBoolean("items." + item.getId() + ".template", false)
                || config.getBoolean("items." + item.getId() + ".hide_from_inventory", false);
    }

    private static File iaContentsFolder() {
        return new File(
                Bukkit.getPluginManager().getPlugin("ItemsAdder").getDataFolder(),
                "contents"
        );
    }

    @EventHandler
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        ItemsAdderAdditions plugin = ItemsAdderAdditions.instance();

        // Step 1: Fetch all items ONCE and share across every item-based system
        List<CustomStack> allItems = ItemsAdder.getAllItems();

        // Step 2: Scan all files ONCE and categorize them
        // From this point forward, no system reads a file from disk or parses YAML.
        ConfigFileRegistry registry = ConfigFileRegistry.scan(iaContentsFolder());

        // Step 3: Rebuild the item-ID cache for O(1) downstream lookups
        NamespaceUtils.buildCache(allItems);

        // Step 4: Tear down world-gen state from the previous cycle
        FurniturePopulatorLoader.clear();
        FurnitureSurfaceDecoratorLoader.clear();

        // Step 5: Item-based systems — pre-filtered by requiredItemSection()
        plugin.actionsManager().reload(allItems);
        plugin.behavioursManager().reload(allItems);

        // Step 6: Creative inventory integration
        reloadCreativeMenuIfEnabled(plugin, allItems);

        // Step 7: Recipes — receives only files with recognized recipe sections
        plugin.recipeManager().reload(registry);

        // ── Step 8: World-gen — each loader receives only its relevant files
        new FurniturePopulatorLoader()
                .loadAll(registry.getFiles(ConfigFileCategory.FURNITURE_POPULATORS));
        new FurnitureSurfaceDecoratorLoader()
                .loadAll(registry.getFiles(ConfigFileCategory.SURFACE_DECORATORS));

        Log.success("Reload", "Reload complete. (files scanned={}, tagged={})",
                registry.totalFilesScanned(), registry.totalFilesTagged());
    }
}
