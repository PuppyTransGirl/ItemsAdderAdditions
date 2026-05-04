package toutouchien.itemsadderadditions.patches;

import org.objectweb.asm.MethodVisitor;

/**
 * Replaces a {@code GETSTATIC} field access with an alternative field name
 * across <em>every</em> method in the target class.
 *
 * @see FieldAccessReplacePatch for a single-method-scoped variant
 */
public abstract class ClassFieldAccessReplacePatch implements ClassPatch {
    protected abstract String targetFieldOwner();

    protected abstract String targetFieldName();

    protected abstract String replacementFieldName();

    @Override
    public final MethodVisitor patchMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        return BytecodeHelper.fieldReplaceVisitor(
                mv, targetFieldOwner(), targetFieldName(), replacementFieldName()
        );
    }
}
