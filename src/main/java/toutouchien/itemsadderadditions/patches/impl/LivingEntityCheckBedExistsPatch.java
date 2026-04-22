package toutouchien.itemsadderadditions.patches.impl;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.patches.MethodPatch;

public final class LivingEntityCheckBedExistsPatch extends MethodPatch {
    @Override
    public String targetClass() {
        return "net/minecraft/world/entity/LivingEntity";
    }

    @Override
    protected String targetMethod() {
        return "checkBedExists";
    }

    @Override
    protected String targetDescriptor() {
        return "()Z";
    }

    @Override
    protected MethodVisitor visitTargetMethod(int access, String name, String descriptor, MethodVisitor mv) {
        return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
            @Override
            protected void onMethodEnter() {
                // 1. Get the Bukkit Entity (this.getBukkitEntity())
                loadThis();
                invokeVirtual(
                        Type.getObjectType("net/minecraft/world/entity/LivingEntity"),
                        new Method("getBukkitEntity", "()Lorg/bukkit/craftbukkit/entity/CraftEntity;")
                );
                int entityLocal = newLocal(Type.getObjectType("org/bukkit/entity/Entity"));
                storeLocal(entityLocal);

                // 1. Get the Plugin Instance via Bukkit.getPluginManager().getPlugin("ItemsAdderAdditions")
                push("ItemsAdderAdditions");
                invokeStatic(
                        Type.getObjectType("org/bukkit/Bukkit"),
                        new Method("getPluginManager", "()Lorg/bukkit/plugin/PluginManager;")
                );
                swap(); // Put manager under string
                invokeInterface(
                        Type.getObjectType("org/bukkit/plugin/PluginManager"),
                        new Method("getPlugin", "(Ljava/lang/String;)Lorg/bukkit/plugin/Plugin;")
                );

                // 2. Get the ClassLoader from that plugin
                invokeVirtual(
                        Type.getType(Object.class),
                        new Method("getClass", "()Ljava/lang/Class;")
                );
                invokeVirtual(
                        Type.getType(Class.class),
                        new Method("getClassLoader", "()Ljava/lang/ClassLoader;")
                );
                int pluginLoader = newLocal(Type.getObjectType("java/lang/ClassLoader"));
                storeLocal(pluginLoader);

                // 3. Class.forName("toutouchien.itemsadderadditions.bridge.BedBridge", true, loader)
                push("toutouchien.itemsadderadditions.bridge.BedBridge");
                push(true);
                loadLocal(pluginLoader);
                invokeStatic(
                        Type.getType(Class.class),
                        new Method("forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")
                );

                // 4. .getMethod("checkBedExists", Entity.class)
                push("checkBedExists");
                push(1);
                newArray(Type.getType(Class.class));
                dup();
                push(0);
                // Use the interface type for the method lookup
                push(Type.getObjectType("org/bukkit/entity/Entity"));
                arrayStore(Type.getType(Class.class));
                invokeVirtual(
                        Type.getType(Class.class),
                        new Method("getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;")
                );

                // 5. .invoke(null, entity)
                push((String) null); // static method has no instance
                push(1);
                newArray(Type.getType(Object.class));
                dup();
                push(0);
                loadLocal(entityLocal);
                arrayStore(Type.getType(Object.class));
                invokeVirtual(
                        Type.getType(java.lang.reflect.Method.class),
                        new Method("invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")
                );

                // 6. Convert result Object -> Boolean -> boolean
                checkCast(Type.getType(Boolean.class));
                invokeVirtual(
                        Type.getType(Boolean.class),
                        new Method("booleanValue", "()Z")
                );

                returnValue();
            }
        };
    }
}
