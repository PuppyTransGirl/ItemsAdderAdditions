package toutouchien.itemsadderadditions.patches;

/**
 * A {@link VersionConstraint} that accepts versions within an inclusive range.
 *
 * <p>Each axis (Minecraft, ItemsAdder) can be constrained independently.
 * Passing {@code null} for a bound means "unbounded" on that side.
 *
 * <pre>{@code
 * // MC 1.20.1 → 1.21.3, any ItemsAdder version
 * VersionRange.mc("1.20.1", "1.21.3")
 *
 * // Any MC, ItemsAdder 3.0.0 → 3.5.9
 * VersionRange.ia("3.0.0", "3.5.9")
 *
 * // MC 1.20.1 → 1.21.3  AND  ItemsAdder 3.0.0 → 3.5.9
 * VersionRange.of("1.20.1", "1.21.3", "3.0.0", "3.5.9")
 *
 * // Only versions at or after 1.21 (no upper bound)
 * VersionRange.mc("1.21", null)
 * }</pre>
 */
public final class VersionRange implements VersionConstraint {
    private final String mcMin; // null = unbounded
    private final String mcMax; // null = unbounded
    private final String iaMin; // null = unbounded
    private final String iaMax; // null = unbounded

    private VersionRange(String mcMin, String mcMax, String iaMin, String iaMax) {
        this.mcMin = mcMin;
        this.mcMax = mcMax;
        this.iaMin = iaMin;
        this.iaMax = iaMax;
    }

    /**
     * Constrain both axes. Any parameter may be {@code null} for "unbounded".
     */
    public static VersionRange of(String mcMin, String mcMax, String iaMin, String iaMax) {
        return new VersionRange(mcMin, mcMax, iaMin, iaMax);
    }

    /**
     * Constrain Minecraft version only; any ItemsAdder version is accepted.
     */
    public static VersionRange mc(String min, String max) {
        return new VersionRange(min, max, null, null);
    }

    /**
     * Constrain ItemsAdder version only; any Minecraft version is accepted.
     */
    public static VersionRange ia(String min, String max) {
        return new VersionRange(null, null, min, max);
    }

    private static boolean inRange(String actual, String min, String max) {
        if (min != null && Version.compareVersionStrings(actual, min) < 0) return false;
        if (max != null && Version.compareVersionStrings(actual, max) > 0) return false;
        return true;
    }

    @Override
    public boolean test(Version version) {
        return inRange(version.minecraft(), mcMin, mcMax)
                && inRange(version.itemsAdder(), iaMin, iaMax);
    }

    @Override
    public String toString() {
        return "VersionRange{mc=[" + mcMin + "," + mcMax + "], ia=[" + iaMin + "," + iaMax + "]}";
    }
}
