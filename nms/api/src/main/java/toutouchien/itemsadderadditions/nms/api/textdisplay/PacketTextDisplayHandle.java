package toutouchien.itemsadderadditions.nms.api.textdisplay;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Identifies a spawned packet text display entity so it can be updated or destroyed later.
 */
@NullMarked
public record PacketTextDisplayHandle(int entityId, UUID uuid) {
}
