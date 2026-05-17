package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayHandle;

/**
 * Holds the packet entity handle for a text display currently shown to a viewer.
 */
@NullMarked
public record TextDisplayVisual(PacketTextDisplayHandle handle) {
}
