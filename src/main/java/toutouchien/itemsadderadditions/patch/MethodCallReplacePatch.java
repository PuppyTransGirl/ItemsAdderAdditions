package toutouchien.itemsadderadditions.patch;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class MethodCallReplacePatch extends MethodPatch {
    protected abstract String targetCallOwner();

    protected abstract String targetCallName();

    protected abstract String replacementCallName();

    @Override
    protected final MethodVisitor visitTargetMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        return new MethodVisitor(Opcodes.ASM9, mv) {
            @Override
            public void visitMethodInsn(
                    int opcode, String owner, String callName,
                    String callDescriptor, boolean isInterface
            ) {
                super.visitMethodInsn(
                        opcode, owner,
                        targetCallOwner().equals(owner) && targetCallName().equals(callName)
                                ? replacementCallName()
                                : callName,
                        callDescriptor, isInterface
                );
            }
        };
    }
}
