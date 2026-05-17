package toutouchien.itemsadderadditions.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MathUtilsTest {
    @Test
    void doubleRoundDown() {
        assertEquals(1.2, MathUtils.decimalRound(1.24, 1));
    }

    @Test
    void doubleRoundUp() {
        assertEquals(1.3, MathUtils.decimalRound(1.26, 1));
    }

    @Test
    void doubleHalfEvenRoundsDownToEven() {
        // 0.25 rounded to 1 decimal: halfway between 0.2 and 0.3 -> HALF_EVEN -> 0.2 (2 is even)
        assertEquals(0.2, MathUtils.decimalRound(0.25, 1));
    }

    @Test
    void doubleHalfEvenRoundsUpToEven() {
        // 0.75 rounded to 1 decimal: halfway between 0.7 and 0.8 -> HALF_EVEN -> 0.8 (8 is even)
        assertEquals(0.8, MathUtils.decimalRound(0.75, 1));
    }

    @Test
    void doubleScale2() {
        assertEquals(1.23, MathUtils.decimalRound(1.234, 2));
    }

    @Test
    void doubleExactValuePreserved() {
        assertEquals(1.0, MathUtils.decimalRound(1.0, 3));
    }

    @Test
    void doubleZeroScaleThrows() {
        assertThrows(IllegalArgumentException.class, () -> MathUtils.decimalRound(1.5, 0));
    }

    @Test
    void doubleNegativeScaleThrows() {
        assertThrows(IllegalArgumentException.class, () -> MathUtils.decimalRound(1.5, -1));
    }


    @Test
    void floatRoundDown() {
        assertEquals(1.2f, MathUtils.decimalRound(1.24f, 1));
    }

    @Test
    void floatRoundUp() {
        assertEquals(1.3f, MathUtils.decimalRound(1.26f, 1));
    }

    @Test
    void floatZeroScaleThrows() {
        assertThrows(IllegalArgumentException.class, () -> MathUtils.decimalRound(1.5f, 0));
    }

    @Test
    void floatNegativeScaleThrows() {
        assertThrows(IllegalArgumentException.class, () -> MathUtils.decimalRound(1.5f, -1));
    }
}
