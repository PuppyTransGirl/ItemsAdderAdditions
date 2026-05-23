package toutouchien.itemsadderadditions.runtime.reload;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReloadResultTest {
    @Test
    void registryChangedTrue() {
        assertTrue(new ReloadResult(true, 5, 3).registryChanged());
    }

    @Test
    void registryChangedFalse() {
        assertFalse(new ReloadResult(false, 5, 3).registryChanged());
    }

    @Test
    void filesScanned() {
        assertEquals(10, new ReloadResult(false, 10, 5).filesScanned());
    }

    @Test
    void filesTagged() {
        assertEquals(4, new ReloadResult(false, 10, 4).filesTagged());
    }

    @Test
    void allZeros() {
        ReloadResult result = new ReloadResult(false, 0, 0);
        assertFalse(result.registryChanged());
        assertEquals(0, result.filesScanned());
        assertEquals(0, result.filesTagged());
    }

    @Test
    void equalRecordsAreEqual() {
        assertEquals(new ReloadResult(true, 3, 2), new ReloadResult(true, 3, 2));
    }

    @Test
    void differentFieldsAreNotEqual() {
        assertNotEquals(new ReloadResult(true, 3, 2), new ReloadResult(false, 3, 2));
        assertNotEquals(new ReloadResult(false, 3, 2), new ReloadResult(false, 4, 2));
        assertNotEquals(new ReloadResult(false, 3, 2), new ReloadResult(false, 3, 1));
    }
}
