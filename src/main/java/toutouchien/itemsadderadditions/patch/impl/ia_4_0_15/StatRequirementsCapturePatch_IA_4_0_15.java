package toutouchien.itemsadderadditions.patch.impl.ia_4_0_15;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.integration.bridge.StatRequirementsBridge;
import toutouchien.itemsadderadditions.patch.InjectPoint;
import toutouchien.itemsadderadditions.patch.MethodInjectPatch;
import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;

public class StatRequirementsCapturePatch_IA_4_0_15 extends MethodInjectPatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.15");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/ll";
    }

    /**
     * ll.a(Entity entity, ia itemEventType, nn executor)
     */
    @Override
    protected String targetMethod() {
        return "a";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/entity/Entity;Litemsadder/m/ia;Litemsadder/m/nn;)V";
    }

    @Override
    protected InjectPoint injectPoint() {
        return InjectPoint.ENTRY;
    }

    /**
     * At entry:
     * - load this  (ll instance)
     * - load arg 0 (Entity)
     * - call StatRequirementsBridge.capture(Object, Object)
     */
    @Override
    protected void inject(GeneratorAdapter ga) {
        ga.loadThis();
        ga.loadArg(0);

        ga.invokeStatic(
                Type.getType(StatRequirementsBridge.class),
                Method.getMethod("void capture(Object, Object)")
        );
    }
}
