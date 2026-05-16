package toutouchien.itemsadderadditions.nms.api.textdisplay;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;

/**
 * Bundles the spawn data for a packet-based text display: where to put it, what to show, and how it looks.
 */
@NullMarked
public record PacketTextDisplay(
        Location location,
        Component text,
        PacketTextDisplayVisual visual
) {
}
