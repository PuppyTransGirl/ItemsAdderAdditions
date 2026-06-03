package toutouchien.itemsadderadditions.integration.valhalla;

import org.jspecify.annotations.NullMarked;

/**
 * ValhallaMMO permanent effect cooldown properties.
 *
 * <p>Serializes to the format used by
 * {@code valhallammo:permanent_effects_cooldown_properties}:
 * <pre>cdrAffected;cooldown</pre>
 */
@NullMarked
public record ValhallaPermanentEffectCooldown(boolean cdrAffected, int cooldown) {
    public String serialize() {
        return cdrAffected + ";" + cooldown;
    }
}
