package toutouchien.itemsadderadditions.patches;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

public abstract class InstanceofBypassPatch extends MethodPatch {
    protected abstract String targetInstanceofType();

    @Override
    protected final MethodVisitor visitTargetMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        GeneratorAdapter ga = new GeneratorAdapter(mv, access, name, descriptor);

        return new MethodVisitor(Opcodes.ASM9, ga) {
            @Override
            public void visitTypeInsn(int opcode, String type) {
                if (opcode == Opcodes.INSTANCEOF &&
                        targetInstanceofType().equals(type)) {
                    // INSTANCEOF consumes objectref and pushes boolean result.
                    // Replace with:
                    // POP objectref
                    // ICONST_1
                    ga.pop();
                    ga.push(true);
                    return;
                }

                super.visitTypeInsn(opcode, type);
            }
        };
    }
}
