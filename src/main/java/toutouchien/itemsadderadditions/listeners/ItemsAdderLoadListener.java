package toutouchien.itemsadderadditions.listeners;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;
import toutouchien.itemsadderadditions.worldgen.FurniturePopulatorLoader;
import toutouchien.itemsadderadditions.worldgen.FurnitureSurfaceDecoratorLoader;

import java.io.File;
import java.util.List;

/**
 * Triggers the full reload cycle whenever ItemsAdder finishes loading its data.
 */
public final class ItemsAdderLoadListener implements Listener {
    @EventHandler
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        ItemsAdderAdditions plugin = ItemsAdderAdditions.instance();
        NmsManager nmsManager = NmsManager.instance();

        List<CustomStack> items = ItemsAdder.getAllItems();

        // Build (or refresh) the CustomStack cache first so all downstream
        // managers can resolve items with O(1) map lookups.
        NamespaceUtils.buildCache(items);

        FurniturePopulatorLoader.clear();
        FurnitureSurfaceDecoratorLoader.clear();

        plugin.actionsManager().reload();
        plugin.behavioursManager().reload();

        if (ItemsAdderAdditions.instance().getConfig().getBoolean("features.creative_inventory_integration", true)
                && nmsManager.handler().creativeMenu() != null) {
            nmsManager.handler().creativeMenu().injectPaintingVariants(items);
            nmsManager.handler().creativeMenu().updatePaintingCache(items);
            plugin.creativeMenuManager().reload();
        }

        plugin.recipeManager().reload();

        File iaDataFolder = new File(
                Bukkit.getPluginManager().getPlugin("ItemsAdder").getDataFolder(),
                "contents"
        );
        FurniturePopulatorLoader.loadAll(iaDataFolder);
        FurnitureSurfaceDecoratorLoader.loadAll(iaDataFolder);

        Log.success("Reload", "Reload complete.");
    }
}
