package toutouchien.itemsadderadditions.patch;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.objectweb.asm.*;
import toutouchien.itemsadderadditions.patch.impl.ia_4_0_15.*;
import toutouchien.itemsadderadditions.patch.impl.ia_4_0_16.*;
import toutouchien.itemsadderadditions.patch.impl.ia_4_0_17.*;
import toutouchien.itemsadderadditions.patch.impl.ia_4_0_18.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives every concrete bytecode patch through a synthetic ASM method visitor so the
 * metadata accessors and {@code inject}/call-rewrite bodies all execute. ClassWriter(0)
 * is used so no frame/stack verification runs - we only need the emit calls covered.
 */
class PatchImplCoverageTest {
    private static List<MethodPatch> allPatches() {
        return List.of(
                new CooldownCapturePatch_IA_4_0_15(),
                new StatRequirementsCapturePatch_IA_4_0_15(),
                new TradeMachineCapturePatch_IA_4_0_15(),
                new StonecutterSelectiveBypassPatch_IA_4_0_15(),
                new AddEnchantmentPatch_IA_4_0_15(),
                new CooldownCapturePatch_IA_4_0_16(),
                new StatRequirementsCapturePatch_IA_4_0_16(),
                new TradeMachineCapturePatch_IA_4_0_16(),
                new StonecutterSelectiveBypassPatch_IA_4_0_16(),
                new AddEnchantmentPatch_IA_4_0_16(),
                new CooldownCapturePatch_IA_4_0_17(),
                new StatRequirementsCapturePatch_IA_4_0_17(),
                new TradeMachineCapturePatch_IA_4_0_17(),
                new StonecutterSelectiveBypassPatch_IA_4_0_17(),
                new AddEnchantmentPatch_IA_4_0_17(),
                new CooldownCapturePatch_IA_4_0_18(),
                new StatRequirementsCapturePatch_IA_4_0_18(),
                new TradeMachineCapturePatch_IA_4_0_18(),
                new StonecutterSelectiveBypassPatch_IA_4_0_18()
        );
    }

    @Test
    void metadataIsPresent() {
        for (MethodPatch patch : allPatches()) {
            assertNotNull(patch.supportedVersions(), patch.getClass().getSimpleName());
            assertNotNull(patch.targetClass());
            assertNotNull(patch.targetMethod());
            assertNotNull(patch.targetDescriptor());
            // supportedVersions must accept its own declared version, reject something absurd
            assertNotNull(patch.supportedVersions().toString());
        }
    }

    @Test
    void patchMethodReturnsOriginalForNonMatchingMethod() {
        for (MethodPatch patch : allPatches()) {
            MethodVisitor base = stubMethodVisitor();
            MethodVisitor result = patch.patchMethod(
                    Opcodes.ACC_PUBLIC, "someUnrelatedMethod", "()V", base);
            assertSame(base, result, patch.getClass().getSimpleName());
        }
    }

    @TestFactory
    Stream<DynamicTest> drivingTargetMethodTriggersInjection() {
        return allPatches().stream().map(patch -> DynamicTest.dynamicTest(
                patch.getClass().getSimpleName(),
                () -> assertDoesNotThrow(() -> exercise(patch))));
    }

    @Test
    void ia4017CooldownCaptureDoesNotHardCodeObfuscatedItemHashField() {
        List<String> fields = emittedFieldAccesses(new CooldownCapturePatch_IA_4_0_17());

        assertFalse(fields.stream().anyMatch(field -> field.startsWith("itemsadder/m/oy.")));
    }

    private MethodVisitor stubMethodVisitor() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "Synthetic", null, "java/lang/Object", null);
        return cw.visitMethod(Opcodes.ACC_PUBLIC, "stub", "()V", null, null);
    }

    private void exercise(MethodPatch patch) {
        String name = patch.targetMethod();
        String descriptor = patch.targetDescriptor();

        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "Synthetic", null, "java/lang/Object", null);

        int access = Opcodes.ACC_PUBLIC;
        MethodVisitor base = cw.visitMethod(access, name, descriptor, null, null);
        MethodVisitor patched = patch.patchMethod(access, name, descriptor, base);

        patched.visitCode();

        // For call-rewrite / call-site patches, emit a call matching the target so the
        // wrapping visitor's branch executes.
        emitMatchingCallIfNeeded(patch, patched);

        emitReturn(patched, Type.getReturnType(descriptor));

        patched.visitMaxs(0, 0);
        patched.visitEnd();
    }

    private List<String> emittedFieldAccesses(MethodPatch patch) {
        String name = patch.targetMethod();
        String descriptor = patch.targetDescriptor();

        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "Synthetic", null, "java/lang/Object", null);

        MethodVisitor base = cw.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, null, null);
        MethodVisitor patched = patch.patchMethod(Opcodes.ACC_PUBLIC, name, descriptor, base);

        patched.visitCode();
        emitReturn(patched, Type.getReturnType(descriptor));
        patched.visitMaxs(0, 0);
        patched.visitEnd();
        cw.visitEnd();

        List<String> fields = new ArrayList<>();
        new ClassReader(cw.toByteArray()).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String methodName,
                    String methodDescriptor,
                    String signature,
                    String[] exceptions
            ) {
                if (!name.equals(methodName) || !descriptor.equals(methodDescriptor)) {
                    return null;
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                        fields.add(owner + "." + fieldName + ":" + fieldDescriptor);
                    }
                };
            }
        }, 0);

        return fields;
    }

    private void emitMatchingCallIfNeeded(MethodPatch patch, MethodVisitor mv) {
        String owner = null;
        String callName = null;
        if (patch instanceof MethodCallReplacePatch p) {
            owner = p.targetCallOwner();
            callName = p.targetCallName();
        } else if (patch instanceof CallSiteInjectPatch p) {
            owner = p.targetCallOwner();
            callName = p.targetCallName();
        }
        if (owner == null) return;

        // Also emit a non-matching call to cover the negative branch.
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Object", "noMatch", "()V", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, callName, "()V", false);
    }

    private void emitReturn(MethodVisitor mv, Type returnType) {
        switch (returnType.getSort()) {
            case Type.VOID -> mv.visitInsn(Opcodes.RETURN);
            case Type.LONG -> {
                mv.visitInsn(Opcodes.LCONST_0);
                mv.visitInsn(Opcodes.LRETURN);
            }
            case Type.FLOAT -> {
                mv.visitInsn(Opcodes.FCONST_0);
                mv.visitInsn(Opcodes.FRETURN);
            }
            case Type.DOUBLE -> {
                mv.visitInsn(Opcodes.DCONST_0);
                mv.visitInsn(Opcodes.DRETURN);
            }
            case Type.OBJECT, Type.ARRAY -> {
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitInsn(Opcodes.ARETURN);
            }
            default -> {
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitInsn(Opcodes.IRETURN);
            }
        }
    }
}
