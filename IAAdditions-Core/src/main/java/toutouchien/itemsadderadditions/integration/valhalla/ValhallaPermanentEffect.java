package toutouchien.itemsadderadditions.integration.valhalla;

import org.jspecify.annotations.NullMarked;

/**
 * One ValhallaMMO permanent potion effect entry.
 *
 * <p>Serializes to the format ValhallaMMO expects in
 * {@code valhallammo:permanent_potion_effects}:
 * <pre>EFFECT:AMPLIFIER:DURATION:TRIGGER</pre>
 */
@NullMarked
public record ValhallaPermanentEffect(String effect, double amplifier, int duration, String condition) {
    public String serialize() {
        return effect + ":" + amplifier + ":" + duration + ":" + condition;
    }
}
