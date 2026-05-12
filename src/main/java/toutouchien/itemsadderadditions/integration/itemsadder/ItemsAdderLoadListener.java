package toutouchien.itemsadderadditions.integration.itemsadder;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

/**
 * Bridges ItemsAdder's data-loaded event into the shared plugin reload pipeline.
 */
@NullMarked
public final class ItemsAdderLoadListener implements Listener {
    @EventHandler
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        ItemsAdderAdditions.instance().reloadItemsAdderData();
    }
}
