package toutouchien.itemsadderadditions.patches;

import org.objectweb.asm.*;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PatchTransformer implements ClassFileTransformer {
    private final Map<String, List<ClassPatch>> patchesByClass;

    public PatchTransformer(List<ClassPatch> patches) {
        this.patchesByClass = patches.stream()
                .collect(Collectors.groupingBy(ClassPatch::targetClass));
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain domain,
            byte[] classfileBuffer
    ) {
        List<ClassPatch> patches = patchesByClass.get(className);
        if (patches == null) return null;

        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES) {
                @Override
                protected ClassLoader getClassLoader() {
                    // Use the actual classloader of the class being transformed
                    // so COMPUTE_FRAMES can resolve its dependencies correctly
                    return loader != null ? loader : ClassLoader.getSystemClassLoader();
                }
            };

// PatchTransformer.java
            reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public MethodVisitor visitMethod(
                        int access, String name, String descriptor,
                        String signature, String[] exceptions
                ) {
                    MethodVisitor mv = super.visitMethod(
                            access, name, descriptor, signature, exceptions
                    );
                    for (ClassPatch patch : patches) {
                        mv = patch.patchMethod(access, name, descriptor, mv);
                    }
                    return mv;
                }
            }, ClassReader.EXPAND_FRAMES); // <-- was 0

            return writer.toByteArray();
        } catch (Exception e) {
            Log.error("PatchTransFormer", "Failed to transform " + className, e);
            return null;
        }
    }
}
