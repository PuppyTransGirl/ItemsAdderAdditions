package toutouchien.itemsadderadditions.nms.api.textdisplay;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record PacketTextDisplayHandle(int entityId, UUID uuid) {
}
