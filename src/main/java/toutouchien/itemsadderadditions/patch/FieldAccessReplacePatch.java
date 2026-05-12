package toutouchien.itemsadderadditions.patch;

import org.objectweb.asm.MethodVisitor;

/**
 * Replaces a {@code GETSTATIC} field access with an alternative field name,
 * scoped to a single target method.
 *
 * @see ClassFieldAccessReplacePatch for an all-methods variant
 */
public abstract class FieldAccessReplacePatch extends MethodPatch {
    protected abstract String targetFieldOwner();

    protected abstract String targetFieldName();

    protected abstract String replacementFieldName();

    @Override
    protected final MethodVisitor visitTargetMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        return BytecodeHelper.fieldReplaceVisitor(
                mv, targetFieldOwner(), targetFieldName(), replacementFieldName()
        );
    }
}
