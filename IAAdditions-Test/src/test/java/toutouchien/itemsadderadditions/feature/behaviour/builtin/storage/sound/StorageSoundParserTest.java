package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.sound;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageSoundParserTest {
    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @Test
    void parse_emptySectionNoSounds_returnsStorageSoundsWithBothNull() {
        StorageSounds result = StorageSoundParser.parse(new YamlConfiguration(), "test:storage");
        assertNotNull(result);
        assertNull(result.open());
        assertNull(result.close());
    }

    @Test
    void parse_validOpenSound_returnsNonNullOpenAndNullClose() {
        YamlConfiguration config = yamlOf(
                """
                        open_sound:
                          name: block.chest.open
                        """
        );
        StorageSounds result = StorageSoundParser.parse(config, "test:storage");
        assertNotNull(result);
        assertNotNull(result.open());
        assertNull(result.close());
    }

    @Test
    void parse_validCloseSound_returnsNullOpenAndNonNullClose() {
        YamlConfiguration config = yamlOf(
                """
                        close_sound:
                          name: block.chest.close
                        """
        );
        StorageSounds result = StorageSoundParser.parse(config, "test:storage");
        assertNotNull(result);
        assertNull(result.open());
        assertNotNull(result.close());
    }

    @Test
    void parse_bothSoundsValid_returnsBothNonNull() {
        YamlConfiguration config = yamlOf(
                """
                        open_sound:
                          name: block.chest.open
                        close_sound:
                          name: block.chest.close
                        """
        );
        StorageSounds result = StorageSoundParser.parse(config, "test:storage");
        assertNotNull(result);
        assertNotNull(result.open());
        assertNotNull(result.close());
    }

    @Test
    void parse_soundWithExplicitSource_preservesSource() {
        YamlConfiguration config = yamlOf(
                """
                        open_sound:
                          name: block.chest.open
                          source: block
                        """
        );
        StorageSounds result = StorageSoundParser.parse(config, "test:storage");
        assertNotNull(result);
        assertNotNull(result.open());
        assertEquals("block", result.open().source().name().toLowerCase());
    }

    @Test
    void parse_soundWithCustomVolumeAndPitch_preservedValues() {
        YamlConfiguration config = yamlOf(
                """
                        open_sound:
                          name: block.chest.open
                          volume: 2.0
                          pitch: 0.5
                        """
        );
        StorageSounds result = StorageSoundParser.parse(config, "test:storage");
        assertNotNull(result);
        assertNotNull(result.open());
        assertEquals(2.0f, result.open().volume(), 0.001f);
        assertEquals(0.5f, result.open().pitch(), 0.001f);
    }

    @Test
    void parse_malformedOpenSoundInvalidSource_returnsNull() {
        YamlConfiguration config = yamlOf(
                """
                        open_sound:
                          name: block.chest.open
                          source: totally_invalid
                        """
        );
        assertNull(StorageSoundParser.parse(config, "test:storage"));
    }

    @Test
    void parse_malformedCloseSoundInvalidSource_returnsNull() {
        YamlConfiguration config = yamlOf(
                """
                        open_sound:
                          name: block.chest.open
                        close_sound:
                          name: block.chest.close
                          source: not_a_source
                        """
        );
        assertNull(StorageSoundParser.parse(config, "test:storage"));
    }

    @Test
    void parse_openSoundMissingName_returnsNull() {
        // A section with no name field - SoundUtils returns null → MALFORMED
        YamlConfiguration config = yamlOf(
                """
                        open_sound:
                          source: block
                        """
        );
        assertNull(StorageSoundParser.parse(config, "test:storage"));
    }
}
