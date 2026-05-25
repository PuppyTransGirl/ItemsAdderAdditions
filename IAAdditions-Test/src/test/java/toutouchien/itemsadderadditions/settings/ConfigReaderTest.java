package toutouchien.itemsadderadditions.settings;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigReaderTest {
    private static ConfigReader readerOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new ConfigReader(cfg);
    }

    @Test
    void bool_setTrue_returnsTrue() {
        assertTrue(readerOf("flag: true").bool("flag", false));
    }

    @Test
    void bool_setFalse_returnsFalse() {
        assertFalse(readerOf("flag: false").bool("flag", true));
    }

    @Test
    void bool_absent_usesDefault() {
        assertTrue(readerOf("").bool("missing", true));
        assertFalse(readerOf("").bool("missing", false));
    }

    @Test
    void bool_wrongType_integer_usesDefault() {
        // YAML integer where boolean expected
        assertFalse(readerOf("flag: 1").bool("flag", false));
    }

    @Test
    void bool_wrongType_string_usesDefault() {
        assertTrue(readerOf("flag: yes_string").bool("flag", true));
    }

    @Test
    void boundedInt_valid_above_min() {
        assertEquals(10, readerOf("n: 10").boundedInt("n", 5, 1));
    }

    @Test
    void boundedInt_exactly_at_min() {
        assertEquals(3, readerOf("n: 3").boundedInt("n", 5, 3));
    }

    @Test
    void boundedInt_below_min_clamped() {
        assertEquals(5, readerOf("n: 2").boundedInt("n", 10, 5));
    }

    @Test
    void boundedInt_absent_usesDefault() {
        assertEquals(7, readerOf("").boundedInt("missing", 7, 0));
    }

    @Test
    void boundedInt_wrongType_string_usesDefault() {
        assertEquals(42, readerOf("n: not_a_number").boundedInt("n", 42, 0));
    }

    @Test
    void boundedInt_doubleValue_truncates() {
        assertEquals(3, readerOf("n: 3.9").boundedInt("n", 0, 0));
    }

    @Test
    void nonBlankString_valid() {
        assertEquals("hello", readerOf("s: hello").nonBlankString("s", "default"));
    }

    @Test
    void nonBlankString_absent_usesDefault() {
        assertEquals("def", readerOf("").nonBlankString("missing", "def"));
    }

    @Test
    void nonBlankString_blank_usesDefault() {
        assertEquals("def", readerOf("s: \"   \"").nonBlankString("s", "def"));
    }

    @Test
    void nonBlankString_wrongType_integer_usesDefault() {
        assertEquals("def", readerOf("s: 42").nonBlankString("s", "def"));
    }

    @Test
    void toggleSection_absent_returnsEmptyMap_with_default() {
        ToggleMap map = readerOf("").toggleSection("features", true);
        assertEquals(0, map.values().size());
        assertTrue(map.defaultValue());
        assertTrue(map.enabled("anything"));
    }

    @Test
    void toggleSection_present_readsBooleanValues() {
        ConfigReader reader = readerOf("""
                features:
                  a: true
                  b: false
                """);
        ToggleMap map = reader.toggleSection("features", false);
        assertTrue(map.enabled("a"));
        assertFalse(map.enabled("b"));
    }

    @Test
    void toggleSection_missingKey_usesDefault() {
        ConfigReader reader = readerOf("""
                features:
                  a: true
                """);
        ToggleMap map = reader.toggleSection("features", false);
        assertFalse(map.enabled("nonexistent")); // falls back to defaultValue=false
    }

    @Test
    void toggleSection_defaultFalse_absent_section() {
        ToggleMap map = readerOf("").toggleSection("actions", false);
        assertFalse(map.defaultValue());
        assertFalse(map.enabled("anything"));
    }
}
