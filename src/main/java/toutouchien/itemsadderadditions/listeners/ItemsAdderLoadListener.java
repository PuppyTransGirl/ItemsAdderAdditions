package toutouchien.itemsadderadditions.listeners;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

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

        plugin.actionsManager().reload();
        plugin.behavioursManager().reload();

        if (ItemsAdderAdditions.instance().getConfig().getBoolean("features.creative_inventory_integration", true)
                && nmsManager.handler().creativeMenu() != null) {
            nmsManager.handler().creativeMenu().injectPaintingVariants(items);
            nmsManager.handler().creativeMenu().updatePaintingCache(items);
            plugin.creativeMenuManager().reload();
        }

        plugin.recipeManager().reload();

        Log.success("Reload", "Reload complete.");
    }
}
