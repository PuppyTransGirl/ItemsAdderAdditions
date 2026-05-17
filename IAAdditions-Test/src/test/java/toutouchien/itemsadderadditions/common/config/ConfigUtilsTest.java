package toutouchien.itemsadderadditions.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigUtilsTest {
    @Test
    void nullValueReturnsNull() {
        assertNull(ConfigUtils.coerceNumber(null, Double.class));
    }

    @Test
    void nonNumericValuePassedThrough() {
        assertEquals("hello", ConfigUtils.coerceNumber("hello", Double.class));
    }

    @Test
    void integerToFloat() {
        Object result = ConfigUtils.coerceNumber(5, Float.class);
        assertInstanceOf(Float.class, result);
        assertEquals(5.0f, (Float) result);
    }

    @Test
    void integerToDouble() {
        Object result = ConfigUtils.coerceNumber(5, Double.class);
        assertInstanceOf(Double.class, result);
        assertEquals(5.0, (Double) result);
    }

    @Test
    void integerToLong() {
        Object result = ConfigUtils.coerceNumber(5, Long.class);
        assertInstanceOf(Long.class, result);
        assertEquals(5L, (Long) result);
    }

    @Test
    void integerToInteger() {
        Object result = ConfigUtils.coerceNumber(5, Integer.class);
        assertInstanceOf(Integer.class, result);
        assertEquals(5, (Integer) result);
    }

    @Test
    void integerToShort() {
        Object result = ConfigUtils.coerceNumber(5, Short.class);
        assertInstanceOf(Short.class, result);
        assertEquals((short) 5, (Short) result);
    }

    @Test
    void integerToByte() {
        Object result = ConfigUtils.coerceNumber(5, Byte.class);
        assertInstanceOf(Byte.class, result);
        assertEquals((byte) 5, (Byte) result);
    }

    @Test
    void doubleToFloat() {
        Object result = ConfigUtils.coerceNumber(5.5, Float.class);
        assertInstanceOf(Float.class, result);
        assertEquals(5.5f, (Float) result);
    }

    @Test
    void doubleToInteger() {
        Object result = ConfigUtils.coerceNumber(5.9, Integer.class);
        assertInstanceOf(Integer.class, result);
        assertEquals(5, (Integer) result);
    }

    @Test
    void nullTargetReturnsValueUnchanged() {
        Object result = ConfigUtils.coerceNumber(42, null);
        assertEquals(42, result);
    }

    @Test
    void unknownTargetReturnsValueUnchanged() {
        Object result = ConfigUtils.coerceNumber(42, String.class);
        assertEquals(42, result);
    }

    @Test
    void primitivePrimitiveFloat() {
        Object result = ConfigUtils.coerceNumber(3, float.class);
        assertInstanceOf(Float.class, result);
        assertEquals(3.0f, (Float) result);
    }

    @Test
    void primitivePrimitiveInt() {
        Object result = ConfigUtils.coerceNumber(3L, int.class);
        assertInstanceOf(Integer.class, result);
        assertEquals(3, (Integer) result);
    }
}
