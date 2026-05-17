package toutouchien.itemsadderadditions.feature.worldgen;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.worldgen.populator.FurniturePopulatorBehaviour;
import toutouchien.itemsadderadditions.feature.worldgen.surface.FurnitureSurfaceDecoratorBehaviour;

/**
 * Ensures that worlds loaded after startup (e.g. via multi-world plugins) also
 * receive furniture populators and surface decorators.
 *
 * <p>Register once in {@code onEnable}.
 */
@NullMarked
public final class FurniturePopulatorWorldListener implements Listener {
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        FurniturePopulatorBehaviour.onWorldLoad(event.getWorld());
        FurnitureSurfaceDecoratorBehaviour.onWorldLoad(event.getWorld());
    }
}
