package toutouchien.itemsadderadditions.patches;

import org.objectweb.asm.MethodVisitor;

/**
 * A single bytecode patch targeting one class.
 *
 * <h3>Version gating</h3>
 * Override {@link #supportedVersions()} to restrict a patch to specific
 * Minecraft / ItemsAdder versions. The default implementation returns
 * {@link VersionConstraint#always()}, meaning the patch applies everywhere.
 *
 * <pre>{@code
 * // Only run on MC 1.20.x with any ItemsAdder version:
 * @Override
 * public VersionConstraint supportedVersions() {
 *     return VersionRange.mc("1.20.0", "1.20.6");
 * }
 *
 * // Only run on exactly these MC versions:
 * @Override
 * public VersionConstraint supportedVersions() {
 *     return VersionSet.mc("1.20.4", "1.21.1");
 * }
 *
 * // Compose constraints:
 * @Override
 * public VersionConstraint supportedVersions() {
 *     return VersionRange.mc("1.20.1", "1.21.3")
 *             .and(VersionRange.ia("3.0.0", "3.5.9"));
 * }
 * }</pre>
 */
public interface ClassPatch {
    /**
     * Internal class name of the class to patch, e.g. {@code "itemsadder/m/lm"}.
     */
    String targetClass();

    /**
     * Declares which runtime versions this patch is compatible with.
     *
     * <p>The default returns {@link VersionConstraint#always()}: the patch
     * applies regardless of Minecraft or ItemsAdder version. Override to
     * restrict compatibility.
     */
    default VersionConstraint supportedVersions() {
        return VersionConstraint.always();
    }

    /**
     * Given the {@link MethodVisitor} for a method in {@link #targetClass()},
     * return either a wrapping visitor that applies the patch, or the original
     * {@code mv} unchanged if this patch does not target that method.
     */
    MethodVisitor patchMethod(int access, String name, String descriptor, MethodVisitor mv);
}
