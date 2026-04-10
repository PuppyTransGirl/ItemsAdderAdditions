package toutouchien.itemsadderadditions.patches;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

public abstract class CallSiteInjectPatch extends MethodPatch {
    protected abstract String targetCallOwner();

    protected abstract String targetCallName();

    protected abstract CallSiteInjectPoint callSiteInjectPoint();

    /**
     * BEFORE_CALL: arguments are not yet on the stack.
     * AFTER_CALL:  return value (if any) is on top of the stack - DUP it
     * first if you need to inspect without consuming it.
     */
    protected abstract void inject(GeneratorAdapter ga);

    @Override
    protected final MethodVisitor visitTargetMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        GeneratorAdapter ga = new GeneratorAdapter(mv, access, name, descriptor);

        return new MethodVisitor(Opcodes.ASM9, ga) {
            @Override
            public void visitMethodInsn(
                    int opcode, String owner, String callName,
                    String callDescriptor, boolean isInterface
            ) {
                boolean matches =
                        targetCallOwner().equals(owner) &&
                                targetCallName().equals(callName);

                if (matches && callSiteInjectPoint() == CallSiteInjectPoint.BEFORE_CALL) {
                    inject(ga);
                }
                super.visitMethodInsn(opcode, owner, callName, callDescriptor, isInterface);
                if (matches && callSiteInjectPoint() == CallSiteInjectPoint.AFTER_CALL) {
                    inject(ga);
                }
            }
        };
    }
}
