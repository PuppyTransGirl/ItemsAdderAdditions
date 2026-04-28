// package toutouchien.itemsadderadditions.patches.impl;

package toutouchien.itemsadderadditions.patches.impl.ia_4_0_16;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.patches.InjectPoint;
import toutouchien.itemsadderadditions.patches.MethodInjectPatch;
import toutouchien.itemsadderadditions.patches.VersionConstraint;
import toutouchien.itemsadderadditions.patches.VersionSet;

import java.util.List;

/**
 * Clears {@code RecipesConfigsLoader.loadingRecipes_craft} at the very end of
 * {@code preLoad()} so that ItemsAdder never registers any crafting recipes.
 * Our own {@link toutouchien.itemsadderadditions.recipes.crafting.CraftingRecipeHandler}
 * takes over instead.
 */
public final class CraftingRecipeBypassPatch_IA_4_0_16 extends MethodInjectPatch {
    private static final Type T_LOADER = Type.getObjectType(
            "dev/lone/itemsadder/Core/loader/RecipesConfigsLoader");
    private static final Type T_LIST = Type.getType(List.class);

    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.16", "4.0.17");
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
        // preLoad(Core, Map, CustomAnvilRepairRecipesManager)
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
        // this.loadingRecipes_craft.clear()
        ga.loadThis();
        ga.getField(T_LOADER, "loadingRecipes_craft", T_LIST);
        ga.invokeInterface(T_LIST, Method.getMethod("void clear()"));
    }
}
