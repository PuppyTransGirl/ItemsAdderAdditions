package toutouchien.itemsadderadditions.patches;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;

public abstract class ConditionalReturnBooleanInjectPatch extends MethodPatch {

    /**
     * Emit bytecode that leaves a boolean on the stack.
     * If that boolean is true, this patch will immediately `return true;`
     * Otherwise execution continues normally.
     */
    protected abstract void pushCondition(GeneratorAdapter ga);

    @Override
    protected final MethodVisitor visitTargetMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
            @Override
            protected void onMethodEnter() {
                Label continueLabel = new Label();

                pushCondition(this); // leaves boolean on stack
                visitJumpInsn(Opcodes.IFEQ, continueLabel);

                push(true);
                visitInsn(Opcodes.IRETURN);

                visitLabel(continueLabel);
            }
        };
    }
}
