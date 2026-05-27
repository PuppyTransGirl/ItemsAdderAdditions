package toutouchien.itemsadderadditions.patch.impl.ia_4_0_17;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.patch.InjectPoint;
import toutouchien.itemsadderadditions.patch.MethodInjectPatch;
import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;

import java.util.List;

/**
 * Clears ItemsAdder's preloaded crafting-table recipes at the end of the
 * obfuscated recipe preload method. IA 4.0.17-beta-12 moved the old
 * RecipesConfigsLoader into itemsadder/m/rn, where FD is the crafting recipe list.
 */
public final class CraftingRecipeBypassPatch_IA_4_0_17 extends MethodInjectPatch {
    private static final Type T_LOADER = Type.getObjectType("itemsadder/m/rn");
    private static final Type T_LIST = Type.getType(List.class);

    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.17");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/rn";
    }

    @Override
    protected String targetMethod() {
        return "a";
    }

    @Override
    protected String targetDescriptor() {
        return "(Litemsadder/m/a;Ljava/util/Map;Litemsadder/m/tc;)V";
    }

    @Override
    protected InjectPoint injectPoint() {
        return InjectPoint.BEFORE_RETURN;
    }

    @Override
    protected void inject(GeneratorAdapter ga) {
        ga.loadThis();
        ga.getField(T_LOADER, "FD", T_LIST);
        ga.invokeInterface(T_LIST, Method.getMethod("void clear()"));
    }
}
