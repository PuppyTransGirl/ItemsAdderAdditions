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
        assertNull(SoundUtils.parseSound(yamlOf("""
                name: "  "
                """)));
    }

    @Test
    void parseSound_bareName_prependsMinecraft() {
        Sound sound = SoundUtils.parseSound(yamlOf("""
                name: block.grass.place
                """));
        assertNotNull(sound);
        assertEquals("minecraft:block.grass.place", sound.name().asString());
    }

    @Test
    void parseSound_fullyQualifiedName_usedAsIs() {
        Sound sound = SoundUtils.parseSound(yamlOf("""
                name: minecraft:block.grass.place
                """));
        assertNotNull(sound);
        assertEquals("minecraft:block.grass.place", sound.name().asString());
    }

    @Test
    void parseSound_customNamespace_preservedInKey() {
        Sound sound = SoundUtils.parseSound(yamlOf("""
                name: mypack:custom_sound
                """));
        assertNotNull(sound);
        assertEquals("mypack:custom_sound", sound.name().asString());
    }

    @Test
    void parseSound_defaultSource_isMaster() {
        Sound sound = SoundUtils.parseSound(yamlOf("""
                name: block.grass.place
                """));
        assertNotNull(sound);
        assertEquals(Sound.Source.MASTER, sound.source());
    }

    @Test
    void parseSound_explicitSourceBlock_parsedCorrectly() {
        Sound sound = SoundUtils.parseSound(yamlOf("""
                name: block.grass.place
                source: block
                """));
        assertNotNull(sound);
        assertEquals(Sound.Source.BLOCK, sound.source());
    }

    @Test
    void parseSound_sourceUppercase_accepted() {
        Sound sound = SoundUtils.parseSound(yamlOf("""
                name: block.grass.place
                source: HOSTILE
                """));
        assertNotNull(sound);
        assertEquals(Sound.Source.HOSTILE, sound.source());
    }

    @Test
    void parseSound_sourceMixedCase_accepted() {
        Sound sound = SoundUtils.parseSound(yamlOf("""
                name: block.grass.place
                source: pLaYeR
                """));
        assertNotNull(sound);
        assertEquals(Sound.Source.PLAYER, sound.source());
    }

    @Test
    void parseSound_invalidSource_returnsNull() {
        assertNull(SoundUtils.parseSound(yamlOf("""
                name: block.grass.place
                source: invalid
                """)));
    }

    @Test
    void parseSound_defaultVolumeAndPitch_areOne() {
        Sound sound = SoundUtils.parseSound(yamlOf("""
                name: block.grass.place
                """));
        assertNotNull(sound);
        assertEquals(1.0f, sound.volume(), 0.001f);
        assertEquals(1.0f, sound.pitch(), 0.001f);
    }

    @Test
    void parseSound_customVolumeAndPitch() {
        Sound sound = SoundUtils.parseSound(yamlOf("""
                name: block.grass.place
                volume: 2.0
                pitch: 0.5
                """));
        assertNotNull(sound);
        assertEquals(2.0f, sound.volume(), 0.001f);
        assertEquals(0.5f, sound.pitch(), 0.001f);
    }

    @Test
    void parseSound_integerVolumeAndPitch_acceptedAsFloat() {
        Sound sound = SoundUtils.parseSound(yamlOf("""
                name: block.grass.place
                volume: 3
                pitch: 2
                """));
        assertNotNull(sound);
        assertEquals(3.0f, sound.volume(), 0.001f);
        assertEquals(2.0f, sound.pitch(), 0.001f);
    }

    @Test
    void parseSound_allFieldsSet_parsedCorrectly() {
        Sound sound = SoundUtils.parseSound(yamlOf("""
                name: minecraft:entity.player.levelup
                source: player
                volume: 1.5
                pitch: 1.2
                """));
        assertNotNull(sound);
        assertEquals("minecraft:entity.player.levelup", sound.name().asString());
        assertEquals(Sound.Source.PLAYER, sound.source());
        assertEquals(1.5f, sound.volume(), 0.001f);
        assertEquals(1.2f, sound.pitch(), 0.001f);
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
