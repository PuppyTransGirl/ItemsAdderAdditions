package toutouchien.itemsadderadditions.patches.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.bridge.StatRequirementsBridge;
import toutouchien.itemsadderadditions.patches.InjectPoint;
import toutouchien.itemsadderadditions.patches.MethodInjectPatch;

public class StatRequirementsCapturePatch extends MethodInjectPatch {

    @Override
    public String targetClass() {
        // obfuscated name of ActionsLoader
        return "itemsadder/m/lm";
    }

    /**
     * ActionsLoader.a(Entity entity, ItemEventType itemEventType, nq nq2)
     */
    @Override
    protected String targetMethod() {
        return "a";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/entity/Entity;Litemsadder/m/ItemEventType;Litemsadder/m/nq;)V";
    }

    @Override
    protected InjectPoint injectPoint() {
        return InjectPoint.ENTRY;
    }

    /**
     * At entry the stack is empty. We push:
     *   this          (arg -1 / load "this")
     *   entity        (arg 0)
     * then call StatRequirementsBridge.capture(Object, Object).
     */
    @Override
    protected void inject(GeneratorAdapter ga) {
        // load `this` (the ActionsLoader instance)
        ga.loadThis();

        // load arg 0 = Entity
        ga.loadArg(0);

        // StatRequirementsBridge.capture(Object actionsLoader, Object entity)
        ga.invokeStatic(
                Type.getType(StatRequirementsBridge.class),
                Method.getMethod("void capture(Object, Object)")
        );
    }
}
