package toutouchien.itemsadderadditions.runtime.reload;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReloadStepResultTest {
    @Test
    void unchangedHasFalseRegistryChangedAndZeroCount() {
        ReloadStepResult result = ReloadStepResult.unchanged("MySystem");
        assertEquals("MySystem", result.system());
        assertFalse(result.registryChanged());
        assertEquals(0, result.loadedCount());
    }

    @Test
    void loadedStoresCount() {
        ReloadStepResult result = ReloadStepResult.loaded("MySystem", 42);
        assertEquals("MySystem", result.system());
        assertFalse(result.registryChanged());
        assertEquals(42, result.loadedCount());
    }

    @Test
    void loadedWithZeroCount() {
        ReloadStepResult result = ReloadStepResult.loaded("Sys", 0);
        assertEquals(0, result.loadedCount());
        assertFalse(result.registryChanged());
    }

    @Test
    void registryTrueStoresFlag() {
        ReloadStepResult result = ReloadStepResult.registry("Sys", true, 7);
        assertEquals("Sys", result.system());
        assertTrue(result.registryChanged());
        assertEquals(7, result.loadedCount());
    }

    @Test
    void registryFalseStoresFlag() {
        ReloadStepResult result = ReloadStepResult.registry("Sys", false, 3);
        assertFalse(result.registryChanged());
        assertEquals(3, result.loadedCount());
    }

    @Test
    void directConstructorStoresAllFields() {
        ReloadStepResult result = new ReloadStepResult("Direct", true, 5);
        assertEquals("Direct", result.system());
        assertTrue(result.registryChanged());
        assertEquals(5, result.loadedCount());
    }

    @Test
    void equalRecordsAreEqual() {
        assertEquals(ReloadStepResult.unchanged("X"), ReloadStepResult.unchanged("X"));
    }

    @Test
    void differentSystemsAreNotEqual() {
        assertNotEquals(ReloadStepResult.unchanged("A"), ReloadStepResult.unchanged("B"));
    }
}
