package toutouchien.itemsadderadditions.patches;

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
     * Emit a static call. signature uses source-style types, e.g.:
     * "void myMethod(int, String)"
     */
    public static void invokeStatic(
            GeneratorAdapter ga, Class<?> owner, String signature
    ) {
        ga.invokeStatic(
                Type.getType(owner),
                Method.getMethod(signature)
        );
    }

    /**
     * Same as invokeStatic but with an internal class name,
     * for when the owner class isn't on your classpath:
     * "com/example/MyHelper"
     */
    public static void invokeStatic(
            GeneratorAdapter ga, String internalOwner, String signature
    ) {
        ga.invokeStatic(
                Type.getObjectType(internalOwner),
                Method.getMethod(signature)
        );
    }

    /**
     * Duplicate the top single-word value (int, float, reference, etc.)
     */
    public static void dup(GeneratorAdapter ga) {
        ga.dup();
    }

    /**
     * Duplicate the top double-word value (long or double)
     */
    public static void dup2(GeneratorAdapter ga) {
        ga.dup2();
    }

    /**
     * Pop the top single-word value
     */
    public static void pop(GeneratorAdapter ga) {
        ga.pop();
    }
}
