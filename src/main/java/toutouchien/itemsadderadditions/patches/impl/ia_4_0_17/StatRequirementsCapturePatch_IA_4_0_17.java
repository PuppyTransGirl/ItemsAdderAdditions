package toutouchien.itemsadderadditions.patches.impl.ia_4_0_17;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.bridge.StatRequirementsBridge;
import toutouchien.itemsadderadditions.patches.InjectPoint;
import toutouchien.itemsadderadditions.patches.MethodInjectPatch;
import toutouchien.itemsadderadditions.patches.VersionConstraint;
import toutouchien.itemsadderadditions.patches.VersionSet;

public class StatRequirementsCapturePatch_IA_4_0_17 extends MethodInjectPatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.17");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/lm";
    }

    @Override
    protected String targetMethod() {
        return "a";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/entity/Entity;Litemsadder/m/ia;Litemsadder/m/nq;)V";
    }

    @Override
    protected InjectPoint injectPoint() {
        return InjectPoint.ENTRY;
    }

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
