package toutouchien.itemsadderadditions.patch.impl.ia_4_0_15;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.integration.bridge.TradeMachineBridge;
import toutouchien.itemsadderadditions.patch.InjectPoint;
import toutouchien.itemsadderadditions.patch.MethodInjectPatch;
import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;

/**
 * Injects into the {@code jp(Plugin, a)} constructor so that
 * {@link TradeMachineBridge#capture(Object)} receives the fully-initialised
 * trade-machine handler instance.
 */
public class TradeMachineCapturePatch_IA_4_0_15 extends MethodInjectPatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.15");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/jp";
    }

    @Override
    protected String targetMethod() {
        return "<init>";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/plugin/Plugin;Litemsadder/m/a;)V";
    }

    @Override
    protected InjectPoint injectPoint() {
        return InjectPoint.BEFORE_RETURN;
    }

    @Override
    protected void inject(GeneratorAdapter ga) {
        ga.loadThis();
        ga.invokeStatic(
                Type.getType(TradeMachineBridge.class),
                Method.getMethod("void capture(Object)")
        );
    }
}
