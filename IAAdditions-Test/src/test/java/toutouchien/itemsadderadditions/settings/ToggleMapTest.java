package toutouchien.itemsadderadditions.settings;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToggleMapTest {
    @Test
    void enabledKeyReturnsTrue() {
        ToggleMap map = new ToggleMap(Map.of("foo", true), false);
        assertTrue(map.enabled("foo"));
    }

    @Test
    void disabledKeyReturnsFalse() {
        ToggleMap map = new ToggleMap(Map.of("foo", false), true);
        assertFalse(map.enabled("foo"));
    }

    @Test
    void missingKeyFallsBackToDefaultTrue() {
        ToggleMap map = new ToggleMap(Map.of(), true);
        assertTrue(map.enabled("missing"));
    }

    @Test
    void missingKeyFallsBackToDefaultFalse() {
        ToggleMap map = new ToggleMap(Map.of(), false);
        assertFalse(map.enabled("missing"));
    }

    @Test
    void originalMapMutationDoesNotAffectToggleMap() {
        Map<String, Boolean> mutable = new HashMap<>();
        mutable.put("a", true);
        ToggleMap map = new ToggleMap(mutable, false);
        mutable.put("a", false);
        assertTrue(map.enabled("a"));
    }

    @Test
    void multipleKeys() {
        ToggleMap map = new ToggleMap(Map.of("a", true, "b", false), true);
        assertTrue(map.enabled("a"));
        assertFalse(map.enabled("b"));
        assertTrue(map.enabled("c")); // default
    }

    @Test
    void valuesAreImmutable() {
        ToggleMap map = new ToggleMap(Map.of("a", true), false);
        assertThrows(UnsupportedOperationException.class, () -> map.values().put("b", true));
    }
}
