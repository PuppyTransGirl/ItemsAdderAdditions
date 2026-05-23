package toutouchien.itemsadderadditions.feature.behaviour.builtin.connectable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectableTypeTest {
    @Test
    void nullReturnsStair() {
        assertEquals(ConnectableType.STAIR, ConnectableType.from(null));
    }

    @Test
    void emptyStringReturnsStair() {
        assertEquals(ConnectableType.STAIR, ConnectableType.from(""));
    }

    @Test
    void tableLowercaseReturnsTable() {
        assertEquals(ConnectableType.TABLE, ConnectableType.from("table"));
    }

    @Test
    void tableUppercaseReturnsTable() {
        assertEquals(ConnectableType.TABLE, ConnectableType.from("TABLE"));
    }

    @Test
    void tableMixedCaseReturnsTable() {
        assertEquals(ConnectableType.TABLE, ConnectableType.from("Table"));
    }

    @Test
    void tableWithLeadingWhitespaceReturnsTable() {
        assertEquals(ConnectableType.TABLE, ConnectableType.from("  table"));
    }

    @Test
    void tableWithTrailingWhitespaceReturnsTable() {
        assertEquals(ConnectableType.TABLE, ConnectableType.from("table  "));
    }

    @Test
    void tableWithBothSidesWhitespaceReturnsTable() {
        assertEquals(ConnectableType.TABLE, ConnectableType.from("  table  "));
    }

    @Test
    void stairStringReturnsStair() {
        assertEquals(ConnectableType.STAIR, ConnectableType.from("stair"));
    }

    @Test
    void unknownStringReturnsStair() {
        assertEquals(ConnectableType.STAIR, ConnectableType.from("xyz"));
    }

    @Test
    void whitespaceOnlyReturnsStair() {
        assertEquals(ConnectableType.STAIR, ConnectableType.from("   "));
    }
}
