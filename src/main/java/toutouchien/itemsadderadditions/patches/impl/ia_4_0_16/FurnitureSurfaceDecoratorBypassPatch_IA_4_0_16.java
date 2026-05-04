package toutouchien.itemsadderadditions.patches.impl.ia_4_0_16;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.bridge.FurnitureSurfaceDecoratorBypassBridge;
import toutouchien.itemsadderadditions.patches.InjectPoint;
import toutouchien.itemsadderadditions.patches.MethodInjectPatch;
import toutouchien.itemsadderadditions.patches.VersionConstraint;
import toutouchien.itemsadderadditions.patches.VersionSet;

/**
 * Injects into {@code uy.i(auo)} at
 * {@link InjectPoint#ENTRY} so that furniture-only {@code surface_decorators}
 * entries are silently removed from IA's view before its loop starts.
 *
 * <p>See {@link FurniturePopulatorBypassPatch_IA_4_0_16} for the full rationale -
 * the strategy is identical, only the target class and bridge differ.
 */
public final class FurnitureSurfaceDecoratorBypassPatch_IA_4_0_16 extends MethodInjectPatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.16", "4.0.17");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/uy";
    }

    @Override
    protected String targetMethod() {
        return "i";
    }

    @Override
    protected String targetDescriptor() {
        return "(Litemsadder/m/auo;)V";
    }

    @Override
    protected InjectPoint injectPoint() {
        return InjectPoint.ENTRY;
    }

    @Override
    protected void inject(GeneratorAdapter ga) {
        ga.loadArg(0);
        ga.invokeStatic(
                Type.getType(FurnitureSurfaceDecoratorBypassBridge.class),
                Method.getMethod("void stripFurnitureKeys(Object)")
        );
    }
}
