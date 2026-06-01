package toutouchien.itemsadderadditions.integration.valhalla;

import org.jspecify.annotations.NullMarked;

/**
 * A single ValhallaMMO attribute stat entry.
 *
 * <p>Serializes to the format ValhallaMMO expects in {@code valhallammo:actual_stats} /
 * {@code valhallammo:default_stats} PDC keys:
 * <pre>STAT:VALUE:OPERATION:HIDDEN</pre>
 *
 * <p>Valid operations: {@code ADD_NUMBER}, {@code ADD_SCALAR}, {@code MULTIPLY_SCALAR_1}
 * (mirrors {@code org.bukkit.attribute.AttributeModifier.Operation} names).
 */
@NullMarked
public record ValhallaStatEntry(String stat, double amount, String operation, boolean hidden) {
    public String serialize() {
        return stat + ":" + amount + ":" + operation + ":" + hidden;
    }
}
