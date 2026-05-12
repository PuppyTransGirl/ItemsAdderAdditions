package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.util.SoundUtils;

/**
 * Parses optional storage open/close sounds from behaviour config.
 */
@NullMarked
public final class StorageSoundParser {
    private static final String LOG_TAG = "Storage";

    private StorageSoundParser() {
    }

    public static @Nullable StorageSounds parse(ConfigurationSection section, String namespacedId) {
        SoundParseResult open = parseSoundField(section, "open_sound", namespacedId);
        if (open.status() == SoundParseStatus.MALFORMED) {
            return null;
        }

        SoundParseResult close = parseSoundField(section, "close_sound", namespacedId);
        if (close.status() == SoundParseStatus.MALFORMED) {
            return null;
        }

        return new StorageSounds(open.sound(), close.sound());
    }

    private static SoundParseResult parseSoundField(ConfigurationSection section, String key, String namespacedId) {
        if (!section.contains(key)) {
            return SoundParseResult.absent();
        }

        ConfigurationSection soundSection = section.getConfigurationSection(key);
        Sound parsed = SoundUtils.parseSound(soundSection);
        if (parsed != null) {
            return SoundParseResult.ok(parsed);
        }

        warnInvalidSource(key, namespacedId, soundSection);
        return SoundParseResult.malformed();
    }

    private static void warnInvalidSource(
            String key,
            String namespacedId,
            @Nullable ConfigurationSection soundSection
    ) {
        if (soundSection == null) {
            return;
        }

        String source = soundSection.getString("source", "");
        if (source.isBlank() || SoundUtils.parseSource(source) != null) {
            return;
        }

        Log.warn(LOG_TAG,
                "storage '{}': invalid {} source '{}' - valid values: master, music, record, weather, "
                        + "block, hostile, neutral, player, ambient, voice, ui",
                namespacedId, key, source);
    }

    private enum SoundParseStatus {
        ABSENT,
        OK,
        MALFORMED
    }

    private record SoundParseResult(@Nullable Sound sound, SoundParseStatus status) {
        static SoundParseResult absent() {
            return new SoundParseResult(null, SoundParseStatus.ABSENT);
        }

        static SoundParseResult ok(Sound sound) {
            return new SoundParseResult(sound, SoundParseStatus.OK);
        }

        static SoundParseResult malformed() {
            return new SoundParseResult(null, SoundParseStatus.MALFORMED);
        }
    }
}
