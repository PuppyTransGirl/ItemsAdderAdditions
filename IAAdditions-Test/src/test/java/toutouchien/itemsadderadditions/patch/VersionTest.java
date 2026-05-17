package toutouchien.itemsadderadditions.patch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionTest {
    @Test
    void normalizeStripsBetaSuffix() {
        assertEquals("4.0.17", Version.normalize("4.0.17-beta-8"));
    }

    @Test
    void normalizeNoSuffixUnchanged() {
        assertEquals("4.0.17", Version.normalize("4.0.17"));
    }

    @Test
    void normalizeWildcardKeptAsIs() {
        assertEquals("*", Version.normalize("*"));
    }

    @Test
    void normalizeNullReturnsNull() {
        assertNull(Version.normalize(null));
    }

    @Test
    void normalizeStripsAlphaSuffix() {
        assertEquals("1.0.0", Version.normalize("1.0.0-alpha-1"));
    }


    @Test
    void compareEqualVersions() {
        assertEquals(0, Version.compareVersionStrings("1.0.0", "1.0.0"));
    }

    @Test
    void compareGreaterMajor() {
        assertTrue(Version.compareVersionStrings("2.0.0", "1.0.0") > 0);
    }

    @Test
    void compareLessMajor() {
        assertTrue(Version.compareVersionStrings("1.0.0", "2.0.0") < 0);
    }

    @Test
    void compareGreaterMinor() {
        assertTrue(Version.compareVersionStrings("1.1.0", "1.0.9") > 0);
    }

    @Test
    void compareWithBetaSuffix() {
        // "1.0.17-beta-8" normalises to "1.0.17" -> equal to "1.0.17"
        assertEquals(0, Version.compareVersionStrings("1.0.17-beta-8", "1.0.17"));
    }

    @Test
    void compareDifferentLengths() {
        assertEquals(0, Version.compareVersionStrings("1.2", "1.2.0"));
    }


    @Test
    void ofCreatesVersion() {
        Version v = Version.of("1.20.4", "4.0.15");
        assertEquals("1.20.4", v.minecraft());
        assertEquals("4.0.15", v.itemsAdder());
    }

    @Test
    void ofBlankMinecraftThrows() {
        assertThrows(IllegalArgumentException.class, () -> Version.of("", "4.0.15"));
    }

    @Test
    void ofBlankItemsAdderThrows() {
        assertThrows(IllegalArgumentException.class, () -> Version.of("1.21", ""));
    }

    @Test
    void ofNullMinecraftThrows() {
        assertThrows(IllegalArgumentException.class, () -> Version.of(null, "4.0.15"));
    }

    @Test
    void ofNullItemsAdderThrows() {
        assertThrows(IllegalArgumentException.class, () -> Version.of("1.21", null));
    }


    @Test
    void anyVersionHasWildcardFields() {
        assertEquals("*", Version.ANY.minecraft());
        assertEquals("*", Version.ANY.itemsAdder());
    }
}
