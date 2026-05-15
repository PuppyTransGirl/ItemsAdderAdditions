package toutouchien.itemsadderadditions.nms.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplay;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayHandle;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayVisual;

@NullMarked
public interface INmsTextDisplayHandler {
    PacketTextDisplayHandle spawn(Player viewer, PacketTextDisplay display);

    void updateMetadata(Player viewer, PacketTextDisplayHandle handle, Component text, PacketTextDisplayVisual visual);

    void destroy(Player viewer, PacketTextDisplayHandle handle);
}
