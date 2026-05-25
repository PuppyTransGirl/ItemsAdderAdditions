package toutouchien.itemsadderadditions.patch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PatchEnumTest {
    @Test
    void injectPointValuesAreStable() {
        assertArrayEquals(new InjectPoint[]{InjectPoint.ENTRY, InjectPoint.BEFORE_RETURN}, InjectPoint.values());
        assertEquals(InjectPoint.ENTRY, InjectPoint.valueOf("ENTRY"));
        assertEquals(InjectPoint.BEFORE_RETURN, InjectPoint.valueOf("BEFORE_RETURN"));
    }

    @Test
    void callSiteInjectPointValuesAreStable() {
        assertArrayEquals(
                new CallSiteInjectPoint[]{CallSiteInjectPoint.BEFORE_CALL, CallSiteInjectPoint.AFTER_CALL},
                CallSiteInjectPoint.values()
        );
        assertEquals(CallSiteInjectPoint.BEFORE_CALL, CallSiteInjectPoint.valueOf("BEFORE_CALL"));
        assertEquals(CallSiteInjectPoint.AFTER_CALL, CallSiteInjectPoint.valueOf("AFTER_CALL"));
    }
}
