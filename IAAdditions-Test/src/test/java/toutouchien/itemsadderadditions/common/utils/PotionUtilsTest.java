package toutouchien.itemsadderadditions.common.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class PotionUtilsTest {
    @BeforeAll
    static void setupServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardownServer() {
        MockBukkit.unmock();
    }

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
    void parsePotionNullSectionReturnsNull() {
        assertNull(PotionUtils.parsePotion(null));
    }

    @Test
    void parsePotionMissingTypeReturnsNull() {
        assertNull(PotionUtils.parsePotion(yamlOf("duration: 20\n")));
    }

    @Test
    void parsePotionBlankTypeReturnsNull() {
        assertNull(PotionUtils.parsePotion(yamlOf("type: '   '\n")));
    }

    @Test
    void parsePotionUnknownTypeReturnsNull() {
        assertNull(PotionUtils.parsePotion(yamlOf("type: definitely_not_an_effect\n")));
    }

    @Test
    void parsePotionValidMinimalUsesDefaults() {
        PotionEffect effect = PotionUtils.parsePotion(yamlOf("type: speed\n"));

        assertNotNull(effect);
        assertEquals(40, effect.getDuration());
        assertEquals(1, effect.getAmplifier());
        assertFalse(effect.isAmbient());
        assertTrue(effect.hasParticles());
        assertTrue(effect.hasIcon());
    }

    @Test
    void parsePotionValidFullConfig() {
        PotionEffect effect = PotionUtils.parsePotion(yamlOf("""
                type: slowness
                duration: 120
                amplifier: 3
                ambient: true
                particles: false
                icon: false
                """));

        assertNotNull(effect);
        assertEquals(120, effect.getDuration());
        assertEquals(3, effect.getAmplifier());
        assertTrue(effect.isAmbient());
        assertFalse(effect.hasParticles());
        assertFalse(effect.hasIcon());
    }
}
