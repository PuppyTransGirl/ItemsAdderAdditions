package toutouchien.itemsadderadditions.patches.impl.ia_4_0_15;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.bridge.CooldownBridge;
import toutouchien.itemsadderadditions.patches.InjectPoint;
import toutouchien.itemsadderadditions.patches.MethodInjectPatch;
import toutouchien.itemsadderadditions.patches.VersionConstraint;
import toutouchien.itemsadderadditions.patches.VersionSet;

public class CooldownCapturePatch_IA_4_0_15 extends MethodInjectPatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.15");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/lq";
    }

    // b(LivingEntity livingEntity, ov ov2)
    @Override
    protected String targetMethod() {
        return "b";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/entity/LivingEntity;Litemsadder/m/ov;)Z";
    }

    @Override
    protected InjectPoint injectPoint() {
        return InjectPoint.BEFORE_RETURN;
    }

    /**
     * Stack at BEFORE_RETURN: boolean result is on top.
     * <p>
     * We call:
     * CooldownBridge.capture(boolean result, Object livingEntity, int itemHash)
     * <p>
     * For IA 4.0.15 the item hash is ov.Bq.
     */
    @Override
    protected void inject(GeneratorAdapter ga) {
        // duplicate original boolean result so one remains for the return path
        ga.dup();

        // arg 0 = LivingEntity
        ga.loadArg(0);

        // arg 1 = ov, field Bq = item hash
        ga.loadArg(1);
        ga.getField(
                Type.getObjectType("itemsadder/m/ov"),
                "Bq",
                Type.INT_TYPE
        );

        ga.invokeStatic(
                Type.getType(CooldownBridge.class),
                Method.getMethod("boolean capture(boolean, Object, int)")
        );
    }
}
