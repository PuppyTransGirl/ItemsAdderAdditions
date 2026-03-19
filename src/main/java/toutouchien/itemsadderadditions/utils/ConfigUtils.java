package toutouchien.itemsadderadditions.utils;

import org.jspecify.annotations.Nullable;

public final class ConfigUtils {
    private ConfigUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Widens or narrows a {@link Number} read from YAML to the Java type
     * expected by a {@code @Parameter}-annotated field.
     *
     * <p>YAML always deserialises integers as {@code Integer} and decimals as
     * {@code Double}; this method lets a field declared as, e.g., {@code Float}
     * or {@code Long} accept those values without a type-mismatch error.
     *
     * @param value  the raw value from the YAML section (may be {@code null} or non-numeric)
     * @param target the expected Java type of the target field
     * @return the coerced value, or the original value unchanged if no conversion applies
     */
    @Nullable
    public static Object coerceNumber(@Nullable Object value, @Nullable Class<?> target) {
        if (!(value instanceof Number n))
            return value;

        return switch (target) {
            case Class<?> c when c == Float.class || c == float.class -> n.floatValue();
            case Class<?> c when c == Double.class || c == double.class -> n.doubleValue();
            case Class<?> c when c == Integer.class || c == int.class -> n.intValue();
            case Class<?> c when c == Long.class || c == long.class -> n.longValue();
            case Class<?> c when c == Short.class || c == short.class -> n.shortValue();
            case Class<?> c when c == Byte.class || c == byte.class -> n.byteValue();
            case null, default -> value;
        };
    }
}
