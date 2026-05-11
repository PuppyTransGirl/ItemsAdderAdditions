package toutouchien.itemsadderadditions.clientcreative;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class ItemsAdderReloadListener implements Listener {
    private final Plugin plugin;
    private final CreativeRegistryService registryService;

    public ItemsAdderReloadListener(Plugin plugin, CreativeRegistryService registryService) {
        this.plugin = plugin;
        this.registryService = registryService;
    }

    @EventHandler
    public void onItemsAdderLoadData(ItemsAdderLoadDataEvent event) {
        // IA can still be finalizing data immediately after the event.
        Bukkit.getScheduler().runTaskLater(plugin, registryService::rebuildAndBroadcast, 20L);
    }
}
