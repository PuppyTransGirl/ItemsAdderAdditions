package toutouchien.itemsadderadditions.patches.impl.ia_4_0_17;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.bridge.CooldownBridge;
import toutouchien.itemsadderadditions.patches.InjectPoint;
import toutouchien.itemsadderadditions.patches.MethodInjectPatch;
import toutouchien.itemsadderadditions.patches.VersionConstraint;
import toutouchien.itemsadderadditions.patches.VersionSet;

public class CooldownCapturePatch_IA_4_0_17 extends MethodInjectPatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.17");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/lr";
    }

    @Override
    protected String targetMethod() {
        return "b";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/entity/LivingEntity;Litemsadder/m/oy;)Z";
    }

    @Override
    protected InjectPoint injectPoint() {
        return InjectPoint.BEFORE_RETURN;
    }

    @Override
    protected void inject(GeneratorAdapter ga) {
        ga.dup();

        ga.loadArg(0);

        ga.loadArg(1);
        ga.getField(
                Type.getObjectType("itemsadder/m/oy"),
                "BL",
                Type.INT_TYPE
        );

        ga.invokeStatic(
                Type.getType(CooldownBridge.class),
                Method.getMethod("boolean capture(boolean, Object, int)")
        );
    }
}
