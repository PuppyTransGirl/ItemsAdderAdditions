package toutouchien.itemsadderadditions.nms.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface INmsBedHandler {
    void placeFakeBed(Player player, Location location);

    void removeFakeBed(Player player, Location location);
}
