package toutouchien.itemsadderadditions.feature.behaviour.builtin.bed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlotOffsetTest {
    @Test
    void parseValidTriple() {
        SlotOffset result = SlotOffset.parse("1,2,3", "test:id");
        assertNotNull(result);
        assertEquals(1, result.dx());
        assertEquals(2, result.dy());
        assertEquals(3, result.dz());
    }

    @Test
    void parseWithWhitespace() {
        SlotOffset result = SlotOffset.parse(" 0 , -1 , 5 ", "test:id");
        assertNotNull(result);
        assertEquals(0, result.dx());
        assertEquals(-1, result.dy());
        assertEquals(5, result.dz());
    }

    @Test
    void parseNegativeValues() {
        SlotOffset result = SlotOffset.parse("-2,-3,-4", "test:id");
        assertNotNull(result);
        assertEquals(-2, result.dx());
        assertEquals(-3, result.dy());
        assertEquals(-4, result.dz());
    }

    @Test
    void parseZeroValues() {
        SlotOffset result = SlotOffset.parse("0,0,0", "test:id");
        assertNotNull(result);
        assertEquals(0, result.dx());
        assertEquals(0, result.dy());
        assertEquals(0, result.dz());
    }

    @Test
    void parseLargeValues() {
        SlotOffset result = SlotOffset.parse("100,-200,300", "test:id");
        assertNotNull(result);
        assertEquals(100, result.dx());
        assertEquals(-200, result.dy());
        assertEquals(300, result.dz());
    }

    @Test
    void parseTwoPartsReturnsNull() {
        assertNull(SlotOffset.parse("1,2", "test:id"));
    }

    @Test
    void parseOnePartReturnsNull() {
        assertNull(SlotOffset.parse("1", "test:id"));
    }

    @Test
    void parseEmptyStringReturnsNull() {
        assertNull(SlotOffset.parse("", "test:id"));
    }

    @Test
    void parseAllNonIntegerReturnsNull() {
        assertNull(SlotOffset.parse("a,b,c", "test:id"));
    }

    @Test
    void parseLastPartNonIntegerReturnsNull() {
        assertNull(SlotOffset.parse("1,2,x", "test:id"));
    }

    @Test
    void parseFourPartsThirdSegmentUnparsableReturnsNull() {
        // split(",", 3) produces ["1", "2", "3,4"] → parseInt("3,4") throws
        assertNull(SlotOffset.parse("1,2,3,4", "test:id"));
    }
}
