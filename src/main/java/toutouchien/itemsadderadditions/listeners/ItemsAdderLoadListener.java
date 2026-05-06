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
import toutouchien.itemsadderadditions.utils.other.Log;
import toutouchien.itemsadderadditions.worldgen.FurniturePopulatorLoader;
import toutouchien.itemsadderadditions.worldgen.FurnitureSurfaceDecoratorLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Triggers the full reload cycle whenever ItemsAdder finishes loading its own data.
 *
 * <h3>Reload order</h3>
 * <ol>
 *   <li>Build the {@link NamespaceUtils} cache so downstream managers can resolve items
 *       in O(1).</li>
 *   <li>Clear world-gen state from the previous cycle.</li>
 *   <li>Reload actions and behaviours (bind new executors from updated YAML).</li>
 *   <li>Reload the creative inventory integration, if enabled and supported by NMS.</li>
 *   <li>Reload recipes.</li>
 *   <li>Load world-gen populators and surface decorators.</li>
 * </ol>
 */
public final class ItemsAdderLoadListener implements Listener {
    /**
     * Reloads the creative menu if both the config feature flag is set
     * and the running NMS version provides a creative-menu handler.
     */
    private static void reloadCreativeMenuIfEnabled(ItemsAdderAdditions plugin, List<CustomStack> items) {
        boolean featureEnabled = plugin.getConfig().getBoolean("features.creative_inventory_integration", true);
        if (!featureEnabled) return;

        NmsManager nms = NmsManager.instance();
        if (nms.handler().creativeMenu() == null) return;

        List<CustomStack> nonHiddenItems = new ArrayList<>();

        for (CustomStack item : items) {
            if (shouldSkip(item)) continue;
            nonHiddenItems.add(item);
        }

        nms.handler().creativeMenu().injectPaintingVariants(nonHiddenItems);
        nms.handler().creativeMenu().updatePaintingCache(nonHiddenItems);

        CreativeMenuManager creativeMenuManager = plugin.creativeMenuManager();
        if (creativeMenuManager != null) {
            creativeMenuManager.reload();
        }
    }

    /**
     * Returns the {@code contents/} directory inside ItemsAdder's data folder.
     */
    private static File iaContentsFolder() {
        return new File(
                Bukkit.getPluginManager().getPlugin("ItemsAdder").getDataFolder(),
                "contents"
        );
    }

    @EventHandler
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        ItemsAdderAdditions plugin = ItemsAdderAdditions.instance();
        List<CustomStack> items = ItemsAdder.getAllItems();

        // 1. Rebuild the item-ID cache so all downstream lookups are O(1).
        NamespaceUtils.buildCache(items);

        // 2. Tear down world-gen state from the last cycle before reloading.
        FurniturePopulatorLoader.clear();
        FurnitureSurfaceDecoratorLoader.clear();

        // 3. Reload the core sub-systems.
        plugin.actionsManager().reload();
        plugin.behavioursManager().reload();

        // 4. Creative inventory integration (optional - requires NMS support and config flag).
        reloadCreativeMenuIfEnabled(plugin, items);

        // 5. Recipes.
        plugin.recipeManager().reload();

        // 6. Register world-gen populators and decorators for all current worlds.
        File iaContents = iaContentsFolder();
        new FurniturePopulatorLoader().loadAll(iaContents);
        new FurnitureSurfaceDecoratorLoader().loadAll(iaContents);

        Log.success("Reload", "Reload complete.");
    }


    /**
     * Returns {@code true} for items that should not appear in the creative
     * menu, for e.g. template items.
     */
    private static boolean shouldSkip(CustomStack item) {
        FileConfiguration config = item.getConfig();
        return config.getBoolean("items." + item.getId() + ".template", false) || config.getBoolean("items." + item.getId() + ".hide_from_inventory", false);
    }
}
