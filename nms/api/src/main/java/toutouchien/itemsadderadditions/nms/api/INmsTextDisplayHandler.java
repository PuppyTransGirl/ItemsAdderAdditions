package toutouchien.itemsadderadditions.nms.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplay;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayHandle;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayVisual;

/**
 * Version-specific handler for sending text display packets to individual players.
 * <p>
 * All operations are packet-only: no entities are added to the world.
 */
@NullMarked
public interface INmsTextDisplayHandler {

    /**
     * Spawns a text display for {@code viewer} and returns a handle for later updates or removal.
     * Sends both the add-entity and entity-data packets in one call.
     */
    PacketTextDisplayHandle spawn(Player viewer, PacketTextDisplay display);

    /**
     * Sends an entity-data update packet for an already-spawned display.
     */
    void updateMetadata(Player viewer, PacketTextDisplayHandle handle, Component text, PacketTextDisplayVisual visual);

    /** Removes the display from {@code viewer}'s client. */
    void destroy(Player viewer, PacketTextDisplayHandle handle);
}
