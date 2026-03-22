package toutouchien.itemsadderadditions.utils.other;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Parses Adventure {@link Sound} objects from YAML configuration sections.
 *
 * <p>Expected section layout:
 * <pre>{@code
 * sound:
 *   name:   "minecraft:block.grass.place"   # required - "minecraft:" prefix optional
 *   source: "master"                         # optional, default: master
 *   volume: 1.0                              # optional, default: 1.0
 *   pitch:  1.0                              # optional, default: 1.0
 * }</pre>
 *
 * <p>Valid {@code source} values (case-insensitive):
 * {@code master}, {@code music}, {@code record}, {@code weather},
 * {@code block}, {@code hostile}, {@code neutral}, {@code player},
 * {@code ambient}, {@code voice}.
 */
@NullMarked
public final class SoundUtils {

    private SoundUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Parses an Adventure {@link Sound} from {@code section}.
     *
     * @param section the YAML section to read from ({@code null} -> returns {@code null})
     * @return the parsed sound, or {@code null} if {@code name} is absent/blank or
     *         {@code source} is not a recognised {@link Sound.Source} name
     */
    @Nullable
    public static Sound parseSound(@Nullable ConfigurationSection section) {
        if (section == null)
            return null;

        String name = section.getString("name", "");
        if (name.isBlank())
            return null;

        // Prepend "minecraft:" when the key has no namespace.
        String keyStr = name.contains(":") ? name : "minecraft:" + name;

        Sound.Source source = parseSource(section.getString("source", "master"));
        if (source == null)
            return null;

        float volume = readFloat(section, "volume", 1.0f);
        float pitch  = readFloat(section, "pitch",  1.0f);

        return Sound.sound(Key.key(keyStr), source, volume, pitch);
    }

    /**
     * Parses a {@link Sound.Source} from a string, ignoring case.
     *
     * @return the matched source, or {@code null} if the string is unrecognised
     */
    public static Sound. @Nullable Source parseSource(@Nullable String raw) {
        if (raw == null || raw.isBlank())
            return Sound.Source.MASTER;

        try {
            return Sound.Source.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Reads a float value from {@code section}, falling back to {@code def} when the
     * key is absent or holds a non-numeric value.
     */
    private static float readFloat(ConfigurationSection section, String key, float def) {
        Object raw = section.get(key, def);
        if (raw instanceof Number n)
            return n.floatValue();
        return def;
    }
}
