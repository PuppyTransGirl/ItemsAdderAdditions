package toutouchien.itemsadderadditions.nms.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public interface INmsBiomeHandler {
    /**
     * Sets every location in {@code locations} to {@code biome}, grouping the writes
     * by chunk, marked dirty, and refreshed exactly once.
     *
     * @param world     the world in which to apply the changes
     * @param locations block locations to update (may span multiple chunks)
     * @param biome     the biome to apply
     */
    void setBiomes(World world, List<Location> locations, Biome biome);
}
