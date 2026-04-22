package toutouchien.itemsadderadditions.patches.impl;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.patches.MethodPatch;

public final class ServerPlayerSleepBypassPatch extends MethodPatch {
    @Override
    public String targetClass() {
        return "net/minecraft/server/level/ServerPlayer";
    }

    @Override
    protected String targetMethod() {
        return "startSleepInBed";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lnet/minecraft/core/BlockPos;Z)Lcom/mojang/datafixers/util/Either;";
    }

    @Override
    protected MethodVisitor visitTargetMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
            @Override
            protected void onMethodEnter() {
                Label continueLabel = new Label(); // block IS a BedBlock → run original method
                Label skipUpdate = new Label();    // sleep failed → skip updateSleepingPlayerList
                Label notPossibleNow = new Label(); // daytime → return Either.left(NOT_POSSIBLE_NOW)
                Label dayCheckPassed = new Label(); // creative or thundering → skip time check

                // Check: is the block at pos a vanilla BedBlock?
                // if (this.level().getBlockState(pos).getBlock() instanceof BedBlock) goto continueLabel
                loadThis();
                invokeVirtual(
                        Type.getObjectType("net/minecraft/server/level/ServerPlayer"),
                        new Method(
                                "level",
                                Type.getObjectType("net/minecraft/server/level/ServerLevel"),
                                new Type[0]
                        )
                );
                loadArg(0);
                invokeVirtual(
                        Type.getObjectType("net/minecraft/server/level/ServerLevel"),
                        new Method(
                                "getBlockState",
                                Type.getObjectType("net/minecraft/world/level/block/state/BlockState"),
                                new Type[]{Type.getObjectType("net/minecraft/core/BlockPos")}
                        )
                );
                invokeVirtual(
                        Type.getObjectType("net/minecraft/world/level/block/state/BlockState"),
                        new Method(
                                "getBlock",
                                Type.getObjectType("net/minecraft/world/level/block/Block"),
                                new Type[0]
                        )
                );
                instanceOf(Type.getObjectType("net/minecraft/world/level/block/BedBlock"));
                visitJumpInsn(Opcodes.IFNE, continueLabel);

                // Daytime / environment guard (mirrors ServerPlayer.startSleepInBed)
                // Obtain the ServerLevel once and keep it in a local for reuse.
                loadThis();
                invokeVirtual(
                        Type.getObjectType("net/minecraft/server/level/ServerPlayer"),
                        new Method(
                                "level",
                                Type.getObjectType("net/minecraft/server/level/ServerLevel"),
                                new Type[0]
                        )
                );
                int levelLocal = newLocal(Type.getObjectType("net/minecraft/server/level/ServerLevel"));
                storeLocal(levelLocal);

                // NOTE: No creative bypass here. Creative players obey the same day/night
                // rules as survival for custom beds - they should be blocked (not kicked).

                // if (level.isThundering()) goto dayCheckPassed
                loadLocal(levelLocal);
                invokeVirtual(
                        Type.getObjectType("net/minecraft/server/level/ServerLevel"),
                        new Method("isThundering", Type.BOOLEAN_TYPE, new Type[0])
                );
                visitJumpInsn(Opcodes.IFNE, dayCheckPassed);

                // long dayTime = level.getDayTime() % 24000L
                loadLocal(levelLocal);
                invokeVirtual(
                        Type.getObjectType("net/minecraft/server/level/ServerLevel"),
                        new Method("getDayTime", Type.LONG_TYPE, new Type[0])
                );
                push(24000L);
                visitInsn(Opcodes.LREM);
                int dayTimeLocal = newLocal(Type.LONG_TYPE);
                storeLocal(dayTimeLocal);

                // if (dayTime < 12541L) goto notPossibleNow
                loadLocal(dayTimeLocal);
                push(12541L);
                ifCmp(Type.LONG_TYPE, LT, notPossibleNow);

                // if (dayTime > 23458L) goto notPossibleNow
                loadLocal(dayTimeLocal);
                push(23458L);
                ifCmp(Type.LONG_TYPE, GT, notPossibleNow);

                visitLabel(dayCheckPassed);

                // Custom bed path
                // Either result = super.startSleepInBed(pos, force);
                loadThis();
                loadArg(0);
                loadArg(1);
                visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        "net/minecraft/world/entity/player/Player",
                        "startSleepInBed",
                        "(Lnet/minecraft/core/BlockPos;Z)Lcom/mojang/datafixers/util/Either;",
                        false
                );

                // Duplicate the Either on the stack so we can inspect it without consuming it.
                // Stack before dup : [..., Either]
                // Stack after  dup : [..., Either, Either]
                visitInsn(Opcodes.DUP);

                // if (!result.right().isPresent()) goto skipUpdate   (sleep failed / problem)
                invokeVirtual(
                        Type.getObjectType("com/mojang/datafixers/util/Either"),
                        new Method("right", Type.getType(java.util.Optional.class), new Type[0])
                );
                invokeVirtual(
                        Type.getType(java.util.Optional.class),
                        new Method("isPresent", Type.BOOLEAN_TYPE, new Type[0])
                );
                visitJumpInsn(Opcodes.IFEQ, skipUpdate);

                // Sleep succeeded - mirror what ServerPlayer.startSleepInBed() normally does
                // this.level().updateSleepingPlayerList();
                //
                // Without this call, SleepStatus.sleepingPlayers is never incremented,
                // so ServerLevel.tick()'s areEnoughSleeping() check always returns false
                // and the night never skips.
                loadThis();
                invokeVirtual(
                        Type.getObjectType("net/minecraft/server/level/ServerPlayer"),
                        new Method(
                                "level",
                                Type.getObjectType("net/minecraft/server/level/ServerLevel"),
                                new Type[0]
                        )
                );
                invokeVirtual(
                        Type.getObjectType("net/minecraft/server/level/ServerLevel"),
                        new Method("updateSleepingPlayerList", Type.VOID_TYPE, new Type[0])
                );

                // Stack here: [..., Either]  (the second copy from DUP, return value)
                visitLabel(skipUpdate);
                visitInsn(Opcodes.ARETURN);

                // Daytime rejection: return Either.left(NOT_POSSIBLE_NOW)
                // PlayerTrySleepBypassPatch reads this Either and will send the vanilla
                // action-bar message to the player before returning false to the caller.
                visitLabel(notPossibleNow);
                visitFieldInsn(
                        Opcodes.GETSTATIC,
                        "net/minecraft/world/entity/player/Player$BedSleepingProblem",
                        "NOT_POSSIBLE_NOW",
                        "Lnet/minecraft/world/entity/player/Player$BedSleepingProblem;"
                );
                invokeStatic(
                        Type.getObjectType("com/mojang/datafixers/util/Either"),
                        new Method(
                                "left",
                                Type.getObjectType("com/mojang/datafixers/util/Either"),
                                new Type[]{Type.getType(Object.class)}
                        )
                );
                visitInsn(Opcodes.ARETURN);

                // Vanilla bed path: fall through to the original ServerPlayer method body
                visitLabel(continueLabel);
            }
        };
    }
}
