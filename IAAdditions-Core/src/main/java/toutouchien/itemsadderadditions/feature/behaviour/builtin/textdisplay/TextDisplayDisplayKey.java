package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * Uniquely identifies one spec display for one owner instance within a viewer's state.
 */
@NullMarked
public record TextDisplayDisplayKey(UUID ownerId, String displayId) {
}
