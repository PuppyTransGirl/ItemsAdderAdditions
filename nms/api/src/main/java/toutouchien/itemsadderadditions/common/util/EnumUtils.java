package toutouchien.itemsadderadditions.common.util;

import com.google.common.base.Preconditions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * Utility class providing static methods for enum manipulations.
 */
@NullMarked
public final class EnumUtils {
    private EnumUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Matches a string to an enum constant, ignoring case.
     *
     * @param key       The string to match
     * @param enumClass The enum class to match against
     * @param <T>       The type of the enum
     * @return An Optional containing the matched enum constant, or empty if no match is found
     */
    public static <T extends Enum<T>> Optional<T> match(@Nullable String key, Class<T> enumClass) {
        T value = match(key, enumClass, null);
        return value == null ? Optional.empty() : Optional.of(value);
    }

    /**
     * Matches a string to an enum constant, ignoring case.
     *
     * @param key          The string to match
     * @param enumClass    The enum class to match against
     * @param defaultValue The default value to return if no match is found
     * @param <T>          The type of the enum
     * @return The matched enum constant, or the default value if no match is found
     */
    @Nullable
    public static <T extends Enum<T>> T match(@Nullable String key, Class<T> enumClass, @Nullable T defaultValue) {
        Preconditions.checkNotNull(enumClass, "enumClass cannot be null");
        if (key == null)
            return defaultValue;

        try {
            return Enum.valueOf(enumClass, key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            for (T constant : enumClass.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(key))
                    return constant;
            }

            return defaultValue;
        }
    }
}
