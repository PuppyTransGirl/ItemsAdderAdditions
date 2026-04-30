package toutouchien.itemsadderadditions.furniture;

import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.UUID;

public final class FurnitureHologramChunkListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() != EntityType.ARMOR_STAND) continue;

            CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entity);
            if (furniture == null) continue;

            String furnitureId = furniture.getNamespacedID();
            if (!FurnitureHologramManager.hasHologram(furnitureId)) continue;

            UUID playerUuid = FurnitureHologramManager.getStoredPlayerUuid(entity);
            Player player = playerUuid != null ? Bukkit.getPlayer(playerUuid) : null;

            FurnitureHologramManager.createHologram(furniture, furnitureId, null, player);
        }
    }
}
