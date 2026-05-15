package toutouchien.itemsadderadditions.nms.api.textdisplay;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PacketTextDisplay(
        Location location,
        Component text,
        PacketTextDisplayVisual visual
) {
}
