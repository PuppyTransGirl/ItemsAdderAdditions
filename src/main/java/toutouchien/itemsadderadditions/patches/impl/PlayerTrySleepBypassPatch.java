package toutouchien.itemsadderadditions.patches.impl;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.patches.MethodPatch;

public final class PlayerTrySleepBypassPatch extends MethodPatch {
    @Override
    public String targetClass() {
        return "org/bukkit/craftbukkit/entity/CraftHumanEntity";
    }

    @Override
    protected String targetMethod() {
        return "sleep";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/Location;Z)Z";
    }

    @Override
    protected MethodVisitor visitTargetMethod(
            int access, String name, String descriptor, MethodVisitor mv
    ) {
        return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
            @Override
            protected void onMethodEnter() {
                Type craftHumanEntityType = Type.getObjectType(
                        "org/bukkit/craftbukkit/entity/CraftHumanEntity"
                );
                Type playerType = Type.getObjectType(
                        "net/minecraft/world/entity/player/Player"
                );
                Type serverLevelType = Type.getObjectType(
                        "net/minecraft/server/level/ServerLevel"
                );
                Type blockPosType = Type.getObjectType("net/minecraft/core/BlockPos");
                Type blockStateType = Type.getObjectType(
                        "net/minecraft/world/level/block/state/BlockState"
                );
                Type blockType = Type.getObjectType(
                        "net/minecraft/world/level/block/Block"
                );
                Type bedBlockType = Type.getObjectType(
                        "net/minecraft/world/level/block/BedBlock"
                );
                Type eitherType = Type.getObjectType(
                        "com/mojang/datafixers/util/Either"
                );
                Type optionalType = Type.getType(java.util.Optional.class);

                int posLocal = newLocal(blockPosType);
                int stateLocal = newLocal(blockStateType);

                Label continueOriginal = newLabel();

                // BlockPos pos = CraftLocation.toBlockPosition(location);
                loadArg(0);
                invokeStatic(
                        Type.getObjectType("org/bukkit/craftbukkit/util/CraftLocation"),
                        new Method(
                                "toBlockPosition",
                                blockPosType,
                                new Type[]{Type.getType("Lorg/bukkit/Location;")}
                        )
                );
                storeLocal(posLocal);

                loadThis();
                invokeVirtual(
                        craftHumanEntityType,
                        new Method("getHandle", playerType, new Type[0])
                );

// Instead of calling level() as a method, check if it's a field or
// use the getter that matches the Paper 1.21 environment.
// In 1.21, Entity.level() returns Level, so we cast to ServerLevel.
                invokeVirtual(
                        playerType,
                        new Method("level", Type.getObjectType("net/minecraft/world/level/Level"), new Type[0])
                );
                checkCast(serverLevelType); // Cast Level to ServerLevel
                loadLocal(posLocal);
                invokeVirtual(
                        serverLevelType,
                        new Method("getBlockState", blockStateType, new Type[]{blockPosType})
                );
                storeLocal(stateLocal);

                // if (state.getBlock() instanceof BedBlock) continue original;
                loadLocal(stateLocal);
                invokeVirtual(
                        blockStateType,
                        new Method("getBlock", blockType, new Type[0])
                );
                instanceOf(bedBlockType);
                ifZCmp(NE, continueOriginal);

                // return !this.getHandle().startSleepInBed(pos, force).left().isPresent();

                loadThis();
                invokeVirtual(
                        craftHumanEntityType,
                        new Method("getHandle", playerType, new Type[0])
                );
                loadLocal(posLocal);
                loadArg(1);
                invokeVirtual(
                        playerType,
                        new Method(
                                "startSleepInBed",
                                eitherType,
                                new Type[]{blockPosType, Type.BOOLEAN_TYPE}
                        )
                );
                // Stack: [Either]
                // Keep a copy so the failure path can extract the problem message.
                dup();
                // Stack: [Either, Either]

                invokeVirtual(
                        eitherType,
                        new Method("left", optionalType, new Type[0])
                );
                invokeVirtual(
                        optionalType,
                        new Method("isPresent", Type.BOOLEAN_TYPE, new Type[0])
                );
                // Stack: [Either, boolean]  - boolean: 1 = left present (failure), 0 = success

                Label sleepSucceeded = newLabel();
                ifZCmp(EQ, sleepSucceeded); // jump when no left (success)
                // Stack: [Either]

                // Failure: mirror CraftHumanEntity.sleep message sending
                // this.getHandle().displayClientMessage(problem.message(), true)
                //
                // BedSleepingProblem is a Java record; the component accessor is message(),
                // NOT getMessage() - using getMessage() throws NoSuchMethodError at runtime
                // and silently swallows the action-bar message (bug fix).
                //
                // Also, some problems (OTHER_PROBLEM, EXPLOSION) have a null message, so we
                // must null-check before calling displayClientMessage (bug fix).
                Type bedProblemType = Type.getObjectType(
                        "net/minecraft/world/entity/player/Player$BedSleepingProblem"
                );
                Type componentType = Type.getObjectType("net/minecraft/network/chat/Component");

                invokeVirtual(eitherType, new Method("left", optionalType, new Type[0]));
                invokeVirtual(optionalType, new Method("get", Type.getType(Object.class), new Type[0]));
                checkCast(bedProblemType);
                // Use the record component accessor "message()" - not "getMessage()"
                invokeVirtual(bedProblemType, new Method("message", componentType, new Type[0]));
                int msgLocal = newLocal(componentType);
                storeLocal(msgLocal);
                // Stack: []

                // Only send the action-bar message if the component is non-null.
                // Problems like OTHER_PROBLEM and EXPLOSION carry a null message and must
                // be silently skipped, otherwise displayClientMessage throws an NPE.
                Label skipMessage = newLabel();
                loadLocal(msgLocal);
                visitJumpInsn(Opcodes.IFNULL, skipMessage);

                loadThis();
                invokeVirtual(craftHumanEntityType, new Method("getHandle", playerType, new Type[0]));
                loadLocal(msgLocal);
                push(true); // overlay = action bar, same as vanilla
                invokeVirtual(
                        playerType,
                        new Method("displayClientMessage", Type.VOID_TYPE,
                                new Type[]{componentType, Type.BOOLEAN_TYPE})
                );

                mark(skipMessage);
                push(false);
                returnValue();

                // Success
                mark(sleepSucceeded);
                // Stack: [Either]
                pop(); // discard the unused Either copy
                push(true);
                returnValue();

                mark(continueOriginal);
            }
        };
    }
}
