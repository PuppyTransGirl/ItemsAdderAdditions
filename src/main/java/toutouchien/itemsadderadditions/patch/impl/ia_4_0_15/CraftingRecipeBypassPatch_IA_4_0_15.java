package toutouchien.itemsadderadditions.patch.impl.ia_4_0_15;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.patch.InjectPoint;
import toutouchien.itemsadderadditions.patch.MethodInjectPatch;
import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;

import java.util.List;

/**
 * Clears RecipesConfigsLoader.loadingRecipes_craft at the end of preLoad()
 * so ItemsAdder does not register crafting recipes itself.
 */
public final class CraftingRecipeBypassPatch_IA_4_0_15
        extends MethodInjectPatch {
    private static final Type T_LOADER = Type.getObjectType(
            "dev/lone/itemsadder/Core/loader/RecipesConfigsLoader");
    private static final Type T_LIST = Type.getType(List.class);

    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.15");
    }

    @Override
    public String targetClass() {
        return "dev/lone/itemsadder/Core/loader/RecipesConfigsLoader";
    }

    @Override
    protected String targetMethod() {
        return "preLoad";
    }

    @Override
    protected String targetDescriptor() {
        return "(Ldev/lone/itemsadder/Core/Core;" +
                "Ljava/util/Map;" +
                "Ldev/lone/itemsadder/Core/recipes/anvil/repair/CustomAnvilRepairRecipesManager;)V";
    }

    @Override
    protected InjectPoint injectPoint() {
        return InjectPoint.BEFORE_RETURN;
    }

    @Override
    protected void inject(GeneratorAdapter ga) {
        ga.loadThis();
        ga.getField(T_LOADER, "loadingRecipes_craft", T_LIST);
        ga.invokeInterface(T_LIST, Method.getMethod("void clear()"));
    }
}
