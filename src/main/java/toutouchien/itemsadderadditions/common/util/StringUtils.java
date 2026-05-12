package toutouchien.itemsadderadditions.common.util;

import com.google.common.base.Preconditions;
import org.jspecify.annotations.NullMarked;

/**
 * Utility class providing static methods for string manipulations.
 */
@NullMarked
public final class StringUtils {
    private StringUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Compares two semantic-version-like strings component-wise.
     *
     * <p>Behavior:
     * - Versions are split on '.' and compared component by component as integers.
     * - Missing components are treated as 0 (e.g. "1.2" == "1.2.0").
     * - Leading zeros are allowed and parsed normally ("01" → 1).
     * - If either component isn't a valid integer, this method throws
     * {@link NumberFormatException}. Callers can catch it if they expect
     * non-numeric parts (pre-releases) or validate beforehand.
     *
     * <p>Limitations:
     * - This is a numeric component comparator only. It does not implement
     * full Semantic Versioning precedence rules (no handling of pre-release
     * identifiers or build metadata).
     *
     * @param a first version string (non-null)
     * @param b second version string (non-null)
     * @return a positive integer if a > b, zero if equal, a negative integer if a < b
     * @throws NullPointerException  if a or b is null
     * @throws NumberFormatException if any numeric component cannot be parsed as an int
     */
    public static int compareSemVer(String a, String b) {
        Preconditions.checkNotNull(a, "a cannot be null");
        Preconditions.checkNotNull(b, "b cannot be null");

        String[] as = a.split("\\.");
        String[] bs = b.split("\\.");

        int n = Math.max(as.length, bs.length);
        for (int i = 0; i < n; i++) {
            // Use replaceAll to remove "-" and any text following it in this segment
            int ai = i < as.length ? parseVersionPart(as[i]) : 0;
            int bi = i < bs.length ? parseVersionPart(bs[i]) : 0;

            if (ai != bi) {
                return Integer.compare(ai, bi);
            }
        }

        return 0;
    }

    private static int parseVersionPart(String part) {
        // Regex: find the first sequence of digits and ignore everything after
        // Example: "3-beta-3" → "3"
        String numericOnly = part.split("\\D")[0];
        return numericOnly.isEmpty() ? 0 : Integer.parseInt(numericOnly);
    }
}
