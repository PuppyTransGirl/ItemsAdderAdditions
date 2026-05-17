package toutouchien.itemsadderadditions.nms.api.textdisplay;

import org.jspecify.annotations.NullMarked;

/**
 * Billboard mode for a text display entity.
 * <ul>
 *   <li>{@code FIXED} - never rotates, faces the direction it was spawned.</li>
 *   <li>{@code VERTICAL} - rotates around the vertical axis to face the player (yaw only).</li>
 *   <li>{@code HORIZONTAL} - rotates around the horizontal axis (pitch only).</li>
 *   <li>{@code CENTER} - always faces the player on all axes.</li>
 * </ul>
 */
@NullMarked
public enum PacketTextDisplayBillboard {
    FIXED,
    VERTICAL,
    HORIZONTAL,
    CENTER
}
