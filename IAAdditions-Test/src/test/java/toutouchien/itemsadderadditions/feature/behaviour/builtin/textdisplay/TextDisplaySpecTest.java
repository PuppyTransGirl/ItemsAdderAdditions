package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextDisplaySpecTest {
    @Test
    void rawTextJoinsLinesWithNewline() {
        TextDisplaySpec spec = new TextDisplaySpec("main", List.of("one", "two", "three"), new Vector(), 0f, 0f, null, 16.0, 20);

        assertEquals("one\ntwo\nthree", spec.rawText());
    }

    @Test
    void rawTextForSingleLineReturnsLine() {
        TextDisplaySpec spec = new TextDisplaySpec("main", List.of("only"), new Vector(), 0f, 0f, null, 16.0, 20);

        assertEquals("only", spec.rawText());
    }

    @Test
    void constructorCopiesTextLines() {
        ArrayList<String> lines = new ArrayList<>(List.of("before"));
        TextDisplaySpec spec = new TextDisplaySpec("main", lines, new Vector(), 0f, 0f, null, 16.0, 20);

        lines.set(0, "after");

        assertEquals(List.of("before"), spec.textLines());
    }

    @Test
    void textLinesAreImmutable() {
        TextDisplaySpec spec = new TextDisplaySpec("main", List.of("line"), new Vector(), 0f, 0f, null, 16.0, 20);

        assertThrows(UnsupportedOperationException.class, () -> spec.textLines().add("x"));
    }

    @Test
    void constructorClonesOffset() {
        Vector offset = new Vector(1, 2, 3);
        TextDisplaySpec spec = new TextDisplaySpec("main", List.of("line"), offset, 0f, 0f, null, 16.0, 20);

        offset.setX(99);

        assertEquals(1.0, spec.offset().getX(), 0.0001);
    }

    @Test
    void accessorsPreserveScalarFields() {
        TextDisplaySpec spec = new TextDisplaySpec("id", List.of("line"), new Vector(1, 2, 3), 45f, 12f, null, 32.5, 80);

        assertEquals("id", spec.id());
        assertEquals(45f, spec.yawOffset(), 0.001f);
        assertEquals(12f, spec.pitchOffset(), 0.001f);
        assertEquals(32.5, spec.viewRange(), 0.0001);
        assertEquals(80, spec.refreshInterval());
    }
}
