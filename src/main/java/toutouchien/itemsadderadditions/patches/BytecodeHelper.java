package toutouchien.itemsadderadditions.patches;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public final class BytecodeHelper {
    private BytecodeHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static void println(GeneratorAdapter ga, String message) {
        ga.getStatic(
                Type.getType(System.class),
                "out",
                Type.getType(java.io.PrintStream.class)
        );
        ga.push(message);
        ga.invokeVirtual(
                Type.getType(java.io.PrintStream.class),
                Method.getMethod("void println(String)")
        );
    }

    /**
     * Emit a static call. {@code signature} uses source-style types, e.g.:
     * {@code "void myMethod(int, String)"}
     */
    public static void invokeStatic(GeneratorAdapter ga, Class<?> owner, String signature) {
        ga.invokeStatic(Type.getType(owner), Method.getMethod(signature));
    }

    /**
     * Same as {@link #invokeStatic(GeneratorAdapter, Class, String)} but with
     * an internal class name for owners not on your classpath, e.g.
     * {@code "com/example/MyHelper"}.
     */
    public static void invokeStatic(GeneratorAdapter ga, String internalOwner, String signature) {
        ga.invokeStatic(Type.getObjectType(internalOwner), Method.getMethod(signature));
    }

    /**
     * Wraps {@code mv} so that every {@code GETSTATIC} of
     * {@code owner.fieldName} is redirected to {@code owner.replacementName}.
     * All other instructions pass through unchanged.
     *
     * <p>Used by both {@link FieldAccessReplacePatch} (single-method scope)
     * and {@link ClassFieldAccessReplacePatch} (all-methods scope) to avoid
     * duplicating the visitor logic.
     */
    static MethodVisitor fieldReplaceVisitor(
            MethodVisitor mv,
            String owner,
            String fieldName,
            String replacementName
    ) {
        return new MethodVisitor(Opcodes.ASM9, mv) {
            @Override
            public void visitFieldInsn(
                    int opcode, String visitedOwner, String visitedName, String descriptor
            ) {
                boolean replace =
                        opcode == Opcodes.GETSTATIC &&
                                owner.equals(visitedOwner) &&
                                fieldName.equals(visitedName);

                super.visitFieldInsn(
                        opcode,
                        visitedOwner,
                        replace ? replacementName : visitedName,
                        descriptor
                );
            }
        };
    }
}
