package toutouchien.itemsadderadditions.patches;

/**
 * Decides whether a patch is compatible with a given runtime {@link Version}.
 *
 * <p>Three built-in implementations cover most needs:
 * <ul>
 *   <li>{@link VersionRange}  - a contiguous range of Minecraft and/or ItemsAdder versions</li>
 *   <li>{@link VersionSet}    - an explicit set of accepted versions</li>
 *   <li>{@link VersionConstraint#always()} - always compatible (default for all patches)</li>
 * </ul>
 *
 * <p>Constraints can be composed:
 * <pre>{@code
 * VersionConstraint.always()                         // every version
 * VersionRange.mc("1.20.1", "1.21.3")               // MC range, any IA version
 * VersionRange.ia("3.0.0", "3.5.9")                 // any MC, IA range
 * VersionRange.of("1.20.1", "1.21.3",
 *                 "3.0.0", "3.5.9")                 // MC + IA range
 * VersionSet.mc("1.20.4", "1.21.1")                 // exact MC versions
 * VersionSet.of(Version.of("1.20.4", "3.2.0"))      // exact MC + IA pair
 * constraint1.and(constraint2)                        // both must match
 * constraint1.or(constraint2)                         // either must match
 * constraint.negate()                                 // invert
 * }</pre>
 */
@FunctionalInterface
public interface VersionConstraint {

    /**
     * A constraint that always returns {@code true}. Use for patches that apply to every version.
     */
    static VersionConstraint always() {
        return v -> true;
    }

    /**
     * A constraint that always returns {@code false}. Useful for temporarily disabling a patch.
     */
    static VersionConstraint never() {
        return v -> false;
    }

    /**
     * Returns {@code true} if this patch should run on the given {@code version}.
     */
    boolean test(Version version);

    default VersionConstraint and(VersionConstraint other) {
        return v -> this.test(v) && other.test(v);
    }

    default VersionConstraint or(VersionConstraint other) {
        return v -> this.test(v) || other.test(v);
    }

    default VersionConstraint negate() {
        return v -> !this.test(v);
    }
}
