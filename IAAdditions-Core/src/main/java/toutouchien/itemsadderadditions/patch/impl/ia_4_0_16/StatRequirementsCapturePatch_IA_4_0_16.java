package toutouchien.itemsadderadditions.patch.impl.ia_4_0_16;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.integration.bridge.StatRequirementsBridge;
import toutouchien.itemsadderadditions.patch.InjectPoint;
import toutouchien.itemsadderadditions.patch.MethodInjectPatch;
import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;

public class StatRequirementsCapturePatch_IA_4_0_16 extends MethodInjectPatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.16");
    }

    @Override
    public String targetClass() {
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
     * this          (arg -1 / load "this")
     * entity        (arg 0)
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
