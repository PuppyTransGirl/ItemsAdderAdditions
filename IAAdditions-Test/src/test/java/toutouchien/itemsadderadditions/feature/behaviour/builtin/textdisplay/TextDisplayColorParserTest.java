package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextDisplayColorParserTest {
    private static Integer parse(Object raw) {
        return TextDisplayColorParser.parse(raw, "ns:id", "display");
    }


    @Test
    void nullInput_returnsNull() {
        assertNull(parse(null));
    }

    @Test
    void booleanFalse_returnsNull() {
        assertNull(parse(false));
    }

    @Test
    void booleanTrue_unsupported_returnsNull() {
        assertNull(parse(true));
    }

    @Test
    void nonStringNonBoolean_returnsNull() {
        assertNull(parse(42));
    }

    @Test
    void stringFalse_caseInsensitive_returnsNull() {
        assertNull(parse("false"));
        assertNull(parse("FALSE"));
    }

    @Test
    void stringNull_returnsNull() {
        assertNull(parse("null"));
    }

    @Test
    void emptyString_returnsNull() {
        assertNull(parse(""));
        assertNull(parse("   "));
    }


    @Test
    void rrggbb_fullyOpaque_returnsArgb() {
        // #FF0000: alpha=0xFF, rgb=0xFF0000 -> (0xFF << 24) | 0xFF0000 = 0xFFFF0000
        Integer result = parse("#FF0000");
        assertNotNull(result);
        assertEquals(0xFFFF0000, result);
    }

    @Test
    void rrggbb_black_returnsFullAlpha() {
        Integer result = parse("#000000");
        assertNotNull(result);
        assertEquals(0xFF000000, result);
    }

    @Test
    void rrggbb_white_returnsFullAlpha() {
        Integer result = parse("#FFFFFF");
        assertNotNull(result);
        assertEquals(0xFFFFFFFF, result);
    }

    @Test
    void rrggbbaa_withHalfAlpha_returnsArgb() {
        // #FF000080: alpha=0x80, rgb=0xFF0000 -> 0x80FF0000
        Integer result = parse("#FF000080");
        assertNotNull(result);
        assertEquals(0x80FF0000, result);
    }

    @Test
    void rrggbbaa_zeroAlpha_returnsTransparent() {
        Integer result = parse("#00000000");
        assertNotNull(result);
        assertEquals(0x00000000, result);
    }

    @Test
    void rrggbb_lowercaseHex_accepted() {
        // Code calls toUpperCase before parsing
        Integer result = parse("#ff0000");
        assertNotNull(result);
        assertEquals(0xFFFF0000, result);
    }

    @Test
    void rrggbb_mixedCase_accepted() {
        Integer result = parse("#Ff0000");
        assertNotNull(result);
        assertEquals(0xFFFF0000, result);
    }


    @Test
    void missingHash_returnsNull() {
        assertNull(parse("FF0000"));
    }

    @Test
    void wrongLength_5hexChars_returnsNull() {
        assertNull(parse("#FFFFF"));
    }

    @Test
    void wrongLength_7hexChars_returnsNull() {
        assertNull(parse("#FFFFFFF"));
    }

    @Test
    void invalidHexChars_returnsNull() {
        assertNull(parse("#ZZZZZZ"));
    }

    @Test
    void partiallyInvalidHex_returnsNull() {
        assertNull(parse("#GG0000"));
    }
}
