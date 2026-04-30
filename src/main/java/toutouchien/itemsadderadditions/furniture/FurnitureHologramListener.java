package toutouchien.itemsadderadditions.furniture;

import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurniturePlaceSuccessEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class FurnitureHologramListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlace(FurniturePlaceSuccessEvent event) {
        var furniture = event.getFurniture();
        var furnitureId = furniture.getNamespacedID();
        var player = event.getPlayer();

        if (!FurnitureHologramManager.hasHologram(furnitureId)) return;

        float placerYaw = player.getLocation().getYaw();
        FurnitureHologramManager.createHologram(furniture, furnitureId, placerYaw, player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        var furniture = event.getFurniture();
        var loc = furniture.getEntity().getLocation();

        FurnitureHologramManager.removeHologram(loc);
    }
}
