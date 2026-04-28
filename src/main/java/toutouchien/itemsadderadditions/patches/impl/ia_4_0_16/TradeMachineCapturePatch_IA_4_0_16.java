package toutouchien.itemsadderadditions.patches.impl.ia_4_0_16;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.bridge.TradeMachineBridge;
import toutouchien.itemsadderadditions.patches.InjectPoint;
import toutouchien.itemsadderadditions.patches.MethodInjectPatch;
import toutouchien.itemsadderadditions.patches.VersionConstraint;
import toutouchien.itemsadderadditions.patches.VersionSet;

/**
 * Injects into the {@code jq(Plugin, a)} constructor so that
 * {@link TradeMachineBridge#capture(Object)} receives the fully-initialised
 * {@code jq} instance right after construction completes.
 *
 * <p>Injecting at {@link InjectPoint#BEFORE_RETURN} (i.e. at the RETURN
 * opcode of the constructor) guarantees that {@code this.vG} and
 * {@code this.vH} have already been assigned before we hand the reference
 * to the bridge.
 */
public class TradeMachineCapturePatch_IA_4_0_16 extends MethodInjectPatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.16", "4.0.17");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/jq";
    }

    // jq(Plugin plugin, a a2)
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
        // Fire after the entire constructor body has run, so vG/vH are set.
        return InjectPoint.BEFORE_RETURN;
    }

    /**
     * Stack is empty (void constructor). We push {@code this} and call
     * {@code TradeMachineBridge.capture(Object)}.
     */
    @Override
    protected void inject(GeneratorAdapter ga) {
        ga.loadThis();
        ga.invokeStatic(
                Type.getType(TradeMachineBridge.class),
                Method.getMethod("void capture(Object)")
        );
    }
}
