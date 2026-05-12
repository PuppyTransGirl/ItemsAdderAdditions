package toutouchien.itemsadderadditions.feature.recipe.stonecutter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.recipe.AbstractRecipeHandler;
import toutouchien.itemsadderadditions.integration.bridge.StonecutterPatchBridge;
import toutouchien.itemsadderadditions.nms.api.NmsManager;

/**
 * Loads and registers custom stonecutter recipes.
 *
 * <h3>YAML structure</h3>
 * <pre>{@code
 * recipes:
 *   stonecutter:
 *     my_recipe:
 *       enabled: true          # optional, default: true
 *       ingredient:
 *         item: "mypack:raw_stone"
 *       result:
 *         item: "mypack:polished_stone"
 *         amount: 1            # optional, default: 1
 * }</pre>
 */
@NullMarked
public final class StonecutterRecipeHandler extends AbstractRecipeHandler {
    public StonecutterRecipeHandler() {
        super("StonecutterRecipe");
    }

    @Override
    protected void registerRecipe(
            String namespace,
            String recipeId,
            ConfigurationSection entry,
            ItemStack ingredient,
            ItemStack result
    ) {
        NmsManager.instance()
                .handler()
                .stonecutterRecipes()
                .register(namespace, recipeId, ingredient, result);

        StonecutterPatchBridge.register(ingredient, result);
        incrementCount();
    }

    @Override
    public void unregisterAll() {
        NmsManager.instance().handler().stonecutterRecipes().unregisterAll();
        StonecutterPatchBridge.clear();
    }
}
