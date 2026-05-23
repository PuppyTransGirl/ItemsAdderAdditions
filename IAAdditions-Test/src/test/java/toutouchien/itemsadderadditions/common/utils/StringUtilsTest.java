package toutouchien.itemsadderadditions.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {
    @Test
    void equalVersions() {
        assertEquals(0, StringUtils.compareSemVer("1.0.0", "1.0.0"));
    }

    @Test
    void greaterMajor() {
        assertTrue(StringUtils.compareSemVer("2.0.0", "1.0.0") > 0);
    }

    @Test
    void lessMajor() {
        assertTrue(StringUtils.compareSemVer("1.0.0", "2.0.0") < 0);
    }

    @Test
    void greaterMinor() {
        assertTrue(StringUtils.compareSemVer("1.1.0", "1.0.0") > 0);
    }

    @Test
    void greaterPatch() {
        assertTrue(StringUtils.compareSemVer("1.0.1", "1.0.0") > 0);
    }

    @Test
    void shorterVersionEqualToLongerWithTrailingZeros() {
        assertEquals(0, StringUtils.compareSemVer("1.2", "1.2.0"));
    }

    @Test
    void shorterVersionEqualToLongerWithTrailingZerosReversed() {
        assertEquals(0, StringUtils.compareSemVer("1.2.0", "1.2"));
    }

    @Test
    void leadingZeroTreatedNumerically() {
        assertEquals(0, StringUtils.compareSemVer("01", "1"));
    }

    @Test
    void preReleaseSuffixStripped() {
        assertEquals(0, StringUtils.compareSemVer("1.0.0-beta-3", "1.0.0"));
    }

    @Test
    void preReleaseSuffixStrippedBothSides() {
        assertEquals(0, StringUtils.compareSemVer("1.0.0-alpha-1", "1.0.0-beta-2"));
    }

    @Test
    void singleComponentGreater() {
        assertTrue(StringUtils.compareSemVer("2", "1") > 0);
    }

    @Test
    void nullAThrows() {
        assertThrows(NullPointerException.class, () -> StringUtils.compareSemVer(null, "1.0"));
    }

    @Test
    void nullBThrows() {
        assertThrows(NullPointerException.class, () -> StringUtils.compareSemVer("1.0", null));
    }

    @Test
    void fourComponentVersions() {
        assertTrue(StringUtils.compareSemVer("1.0.0.1", "1.0.0.0") > 0);
    }

    @Test
    void emptyStringsAreEqual() {
        assertEquals(0, StringUtils.compareSemVer("", ""));
    }

    @Test
    void emptyStringEqualsZero() {
        assertEquals(0, StringUtils.compareSemVer("", "0"));
    }

    @Test
    void multipleLeadingZerosIgnored() {
        assertEquals(0, StringUtils.compareSemVer("001.002.003", "1.2.3"));
    }

    @Test
    void twoDigitMajorBeatsOneDigit() {
        assertTrue(StringUtils.compareSemVer("10.0.0", "9.9.9") > 0);
    }

    @Test
    void fiveComponentVersions() {
        assertTrue(StringUtils.compareSemVer("1.0.0.0.1", "1.0.0.0.0") > 0);
    }

    @Test
    void singleComponentLess() {
        assertTrue(StringUtils.compareSemVer("1", "2") < 0);
    }

    @Test
    void preReleaseLargerNumericStillEqual() {
        assertEquals(0, StringUtils.compareSemVer("2.0.0-beta-9", "2.0.0-rc-1"));
    }
}
