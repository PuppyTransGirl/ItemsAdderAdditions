package toutouchien.itemsadderadditions.patch;

import org.objectweb.asm.MethodVisitor;

public abstract class MethodPatch implements ClassPatch {
    protected abstract String targetMethod();

    protected abstract String targetDescriptor();

    protected abstract MethodVisitor visitTargetMethod(
            int access, String name, String descriptor, MethodVisitor mv
    );

    @Override
    public final MethodVisitor patchMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        if (targetMethod().equals(name) && targetDescriptor().equals(descriptor)) {
            return visitTargetMethod(access, name, descriptor, mv);
        }
        return mv;
    }
}
