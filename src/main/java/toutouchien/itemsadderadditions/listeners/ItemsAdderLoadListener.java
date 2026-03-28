package toutouchien.itemsadderadditions.listeners;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.creative.PacketListener;
import toutouchien.itemsadderadditions.creative.RegistryInjector;
import toutouchien.itemsadderadditions.utils.Log;
import toutouchien.itemsadderadditions.utils.other.VersionUtils;

import java.util.List;

/**
 * Triggers the full reload cycle whenever ItemsAdder finishes loading its data.
 */
public final class ItemsAdderLoadListener implements Listener {

    @EventHandler
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        ItemsAdderAdditions plugin = ItemsAdderAdditions.instance();
        List<CustomStack> items = ItemsAdder.getAllItems();

        plugin.actionsManager().reload();
        plugin.behavioursManager().reload();

        if (VersionUtils.isHigherThanOrEquals(VersionUtils.v1_21_11)) {
            if (ItemsAdderAdditions.instance().getConfig().getBoolean("features.creative_inventory_integration", false)) {
                RegistryInjector.injectPaintingVariants(items);
                PacketListener.updateCache(items);
                plugin.creativeMenuManager().reload();
            }
        }

        Log.success("IAA", "Reload complete.");
    }
}
