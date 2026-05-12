package toutouchien.itemsadderadditions.patch;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

public abstract class MethodCallRewritePatch extends MethodPatch {
    protected abstract String targetCallOwner();

    protected abstract String targetCallName();

    protected abstract String targetCallDescriptor();

    protected abstract void rewriteCall(
            GeneratorAdapter ga,
            int opcode,
            String owner,
            String name,
            String descriptor,
            boolean isInterface
    );

    @Override
    protected final MethodVisitor visitTargetMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        GeneratorAdapter ga = new GeneratorAdapter(mv, access, name, descriptor);

        return new MethodVisitor(Opcodes.ASM9, ga) {
            @Override
            public void visitMethodInsn(
                    int opcode,
                    String owner,
                    String callName,
                    String callDescriptor,
                    boolean isInterface
            ) {
                boolean matches =
                        targetCallOwner().equals(owner) &&
                                targetCallName().equals(callName) &&
                                targetCallDescriptor().equals(callDescriptor);

                if (matches) {
                    rewriteCall(
                            ga,
                            opcode,
                            owner,
                            callName,
                            callDescriptor,
                            isInterface
                    );
                    return;
                }

                super.visitMethodInsn(
                        opcode,
                        owner,
                        callName,
                        callDescriptor,
                        isInterface
                );
            }
        };
    }
}
