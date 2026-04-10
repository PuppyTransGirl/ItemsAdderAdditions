package toutouchien.itemsadderadditions.patches;

import org.objectweb.asm.MethodVisitor;

public interface ClassPatch {
    String targetClass();

    // access added so base classes can build AdviceAdapter/GeneratorAdapter
    MethodVisitor patchMethod(
            int access, String name, String descriptor, MethodVisitor mv
    );
}
