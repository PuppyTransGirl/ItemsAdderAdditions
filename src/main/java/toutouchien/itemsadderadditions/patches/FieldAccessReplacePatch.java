package toutouchien.itemsadderadditions.patches;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class FieldAccessReplacePatch extends MethodPatch {
    protected abstract String targetFieldOwner();

    protected abstract String targetFieldName();

    protected abstract String replacementFieldName();

    @Override
    protected final MethodVisitor visitTargetMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        return new MethodVisitor(Opcodes.ASM9, mv) {
            @Override
            public void visitFieldInsn(
                    int opcode, String owner, String fieldName, String fieldDescriptor
            ) {
                boolean replace =
                        opcode == Opcodes.GETSTATIC &&
                                targetFieldOwner().equals(owner) &&
                                targetFieldName().equals(fieldName);

                super.visitFieldInsn(
                        opcode,
                        owner,
                        replace ? replacementFieldName() : fieldName,
                        fieldDescriptor
                );
            }
        };
    }
}
