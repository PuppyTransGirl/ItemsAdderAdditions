package toutouchien.itemsadderadditions.utils;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.index.qual.Positive;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for mathematical operations.
 */
public final class MathUtils {
    private MathUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Rounds a double value to the specified number of decimal places using HALF_EVEN rounding mode.
     *
     * @param value The value to round.
     * @param scale The number of decimal places to round to (must be positive).
     * @return The rounded value.
     * @throws IllegalArgumentException if scale is less than 1.
     */
    public static double decimalRound(double value, @Positive int scale) {
        Preconditions.checkArgument(scale >= 1, "scale cannot be less than 1: %s", scale);

        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_EVEN).doubleValue();
    }

    /**
     * Rounds a float value to the specified number of decimal places using HALF_EVEN rounding mode.
     *
     * @param value The value to round.
     * @param scale The number of decimal places to round to (must be positive).
     * @return The rounded value.
     * @throws IllegalArgumentException if scale is less than 1.
     */
    public static float decimalRound(float value, @Positive int scale) {
        Preconditions.checkArgument(scale >= 1, "scale cannot be less than 1: %s", scale);

        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_EVEN).floatValue();
    }
}
