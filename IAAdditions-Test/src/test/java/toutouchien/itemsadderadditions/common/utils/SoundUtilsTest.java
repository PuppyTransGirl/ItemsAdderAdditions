package toutouchien.itemsadderadditions.common.utils;

import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoundUtilsTest {
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
    void parseSound_nullSection_returnsNull() {
        assertNull(SoundUtils.parseSound(null));
    }

    @Test
    void parseSound_emptySection_returnsNull() {
        assertNull(SoundUtils.parseSound(new YamlConfiguration()));
    }

    @Test
    void parseSound_blankName_returnsNull() {
        YamlConfiguration yaml = yamlOf("name: \"  \"");
        assertNull(SoundUtils.parseSound(yaml));
    }

    @Test
    void parseSound_bareName_prependsMinecraft() {
        YamlConfiguration yaml = yamlOf("name: block.grass.place");
        Sound sound = SoundUtils.parseSound(yaml);
        assertNotNull(sound);
        assertEquals("minecraft:block.grass.place", sound.name().asString());
    }

    @Test
    void parseSound_fullyQualifiedName_usedAsIs() {
        YamlConfiguration yaml = yamlOf("name: minecraft:block.grass.place");
        Sound sound = SoundUtils.parseSound(yaml);
        assertNotNull(sound);
        assertEquals("minecraft:block.grass.place", sound.name().asString());
    }

    @Test
    void parseSound_defaultSource_isMaster() {
        YamlConfiguration yaml = yamlOf("name: block.grass.place");
        Sound sound = SoundUtils.parseSound(yaml);
        assertNotNull(sound);
        assertEquals(Sound.Source.MASTER, sound.source());
    }

    @Test
    void parseSound_explicitSourceBlock_parsedCorrectly() {
        YamlConfiguration yaml = yamlOf("name: block.grass.place\nsource: block");
        Sound sound = SoundUtils.parseSound(yaml);
        assertNotNull(sound);
        assertEquals(Sound.Source.BLOCK, sound.source());
    }

    @Test
    void parseSound_sourceUppercase_accepted() {
        YamlConfiguration yaml = yamlOf("name: block.grass.place\nsource: HOSTILE");
        Sound sound = SoundUtils.parseSound(yaml);
        assertNotNull(sound);
        assertEquals(Sound.Source.HOSTILE, sound.source());
    }

    @Test
    void parseSound_invalidSource_returnsNull() {
        YamlConfiguration yaml = yamlOf("name: block.grass.place\nsource: invalid");
        assertNull(SoundUtils.parseSound(yaml));
    }

    @Test
    void parseSound_customVolumeAndPitch() {
        YamlConfiguration yaml = yamlOf("name: block.grass.place\nvolume: 2.0\npitch: 0.5");
        Sound sound = SoundUtils.parseSound(yaml);
        assertNotNull(sound);
        assertEquals(2.0f, sound.volume(), 0.001f);
        assertEquals(0.5f, sound.pitch(), 0.001f);
    }

    @Test
    void parseSound_defaultVolumeAndPitch_areOne() {
        YamlConfiguration yaml = yamlOf("name: block.grass.place");
        Sound sound = SoundUtils.parseSound(yaml);
        assertNotNull(sound);
        assertEquals(1.0f, sound.volume(), 0.001f);
        assertEquals(1.0f, sound.pitch(), 0.001f);
    }

    @Test
    void parseSound_customNamespace_preservedInKey() {
        YamlConfiguration yaml = yamlOf("name: mypack:custom_sound");
        Sound sound = SoundUtils.parseSound(yaml);
        assertNotNull(sound);
        assertEquals("mypack:custom_sound", sound.name().asString());
    }


    @Test
    void parseSource_null_returnsMaster() {
        assertEquals(Sound.Source.MASTER, SoundUtils.parseSource(null));
    }

    @Test
    void parseSource_emptyString_returnsMaster() {
        assertEquals(Sound.Source.MASTER, SoundUtils.parseSource(""));
        assertEquals(Sound.Source.MASTER, SoundUtils.parseSource("   "));
    }

    @Test
    void parseSource_master_returnsMaster() {
        assertEquals(Sound.Source.MASTER, SoundUtils.parseSource("master"));
    }

    @Test
    void parseSource_allValidSources() {
        assertEquals(Sound.Source.MUSIC, SoundUtils.parseSource("music"));
        assertEquals(Sound.Source.RECORD, SoundUtils.parseSource("record"));
        assertEquals(Sound.Source.WEATHER, SoundUtils.parseSource("weather"));
        assertEquals(Sound.Source.BLOCK, SoundUtils.parseSource("block"));
        assertEquals(Sound.Source.HOSTILE, SoundUtils.parseSource("hostile"));
        assertEquals(Sound.Source.NEUTRAL, SoundUtils.parseSource("neutral"));
        assertEquals(Sound.Source.PLAYER, SoundUtils.parseSource("player"));
        assertEquals(Sound.Source.AMBIENT, SoundUtils.parseSource("ambient"));
        assertEquals(Sound.Source.VOICE, SoundUtils.parseSource("voice"));
    }

    @Test
    void parseSource_uppercase_parsedCorrectly() {
        assertEquals(Sound.Source.BLOCK, SoundUtils.parseSource("BLOCK"));
    }

    @Test
    void parseSource_unknownString_returnsNull() {
        assertNull(SoundUtils.parseSource("unknown_source"));
    }
}
