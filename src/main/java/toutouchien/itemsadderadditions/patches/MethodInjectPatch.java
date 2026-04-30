package toutouchien.itemsadderadditions.patches;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;

public abstract class MethodInjectPatch extends MethodPatch {
    protected abstract InjectPoint injectPoint();

    /**
     * Emit instructions here. Do NOT emit a return.
     */
    protected abstract void inject(GeneratorAdapter ga);

    @Override
    protected final MethodVisitor visitTargetMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        return switch (injectPoint()) {
            case ENTRY -> new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                @Override
                protected void onMethodEnter() {
                    inject(this); // AdviceAdapter extends GeneratorAdapter
                }
            };
            case BEFORE_RETURN -> new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                @Override
                protected void onMethodExit(int opcode) {
                    if (opcode != Opcodes.ATHROW) {
                        inject(this);
                    }
                }
            };
        };
    }
}
