package toutouchien.itemsadderadditions.common.utils;

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
     * - The core version (everything before the first '-') is split on '.' and
     * compared component by component as integers.
     * - Missing components are treated as 0 (e.g. "1.2" == "1.2.0").
     * - Leading zeros are allowed and parsed normally ("01" -> 1).
     * - Pre-release precedence follows Semantic Versioning: a version with a
     * pre-release suffix has lower precedence than the same core version
     * without one (e.g. "1.0.10-beta-14" < "1.0.10"). The core version still
     * dominates, so "1.0.11-beta-3" > "1.0.10".
     * - Two pre-releases on the same core version are compared by their dot- and
     * dash-separated identifiers per SemVer (numeric ascending, otherwise
     * lexical; numeric identifiers rank below non-numeric ones).
     *
     * @param a first version string (non-null)
     * @param b second version string (non-null)
     * @return a positive integer if a > b, zero if equal, a negative integer if a < b
     * @throws NullPointerException if a or b is null
     */
    public static int compareSemVer(String a, String b) {
        Preconditions.checkNotNull(a, "a cannot be null");
        Preconditions.checkNotNull(b, "b cannot be null");

        int aDash = a.indexOf('-');
        int bDash = b.indexOf('-');

        String aCore = aDash < 0 ? a : a.substring(0, aDash);
        String bCore = bDash < 0 ? b : b.substring(0, bDash);

        int coreCmp = compareCore(aCore, bCore);
        if (coreCmp != 0)
            return coreCmp;

        String aPre = aDash < 0 ? "" : a.substring(aDash + 1);
        String bPre = bDash < 0 ? "" : b.substring(bDash + 1);

        // A version without a pre-release outranks one with a pre-release.
        if (aPre.isEmpty() || bPre.isEmpty())
            return Boolean.compare(aPre.isEmpty(), bPre.isEmpty());

        return comparePreRelease(aPre, bPre);
    }

    private static int compareCore(String a, String b) {
        String[] as = a.split("\\.");
        String[] bs = b.split("\\.");

        int n = Math.max(as.length, bs.length);
        for (int i = 0; i < n; i++) {
            int ai = i < as.length ? parseVersionPart(as[i]) : 0;
            int bi = i < bs.length ? parseVersionPart(bs[i]) : 0;

            if (ai != bi)
                return Integer.compare(ai, bi);
        }

        return 0;
    }

    private static int comparePreRelease(String a, String b) {
        String[] as = a.split("[.\\-]");
        String[] bs = b.split("[.\\-]");

        int n = Math.max(as.length, bs.length);
        for (int i = 0; i < n; i++) {
            // Fewer identifiers means lower precedence when all preceding ones are equal.
            if (i >= as.length)
                return -1;
            if (i >= bs.length)
                return 1;

            String ap = as[i];
            String bp = bs[i];
            boolean aNum = isNumeric(ap);
            boolean bNum = isNumeric(bp);

            if (aNum && bNum) {
                int cmp = Integer.compare(Integer.parseInt(ap), Integer.parseInt(bp));
                if (cmp != 0)
                    return cmp;
            } else if (aNum != bNum) {
                // Numeric identifiers always have lower precedence than non-numeric.
                return aNum ? -1 : 1;
            } else {
                int cmp = ap.compareTo(bp);
                if (cmp != 0)
                    return cmp;
            }
        }

        return 0;
    }

    private static boolean isNumeric(String s) {
        if (s.isEmpty())
            return false;

        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i)))
                return false;
        }

        return true;
    }

    private static int parseVersionPart(String part) {
        // Find the first sequence of digits and ignore everything after.
        // Example: "3-beta-3" -> "3"
        String numericOnly = part.split("\\D")[0];
        return numericOnly.isEmpty() ? 0 : Integer.parseInt(numericOnly);
    }
}
