package toutouchien.itemsadderadditions.recipes.campfire;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.recipes.AbstractRecipeHandler;

/**
 * Loads and registers custom campfire cooking recipes.
 *
 * <h3>YAML structure</h3>
 * <pre>{@code
 * recipes:
 *   campfire_cooking:
 *     my_recipe:
 *       enabled: true          # optional, default: true
 *       ingredient:
 *         item: "mypack:raw_fish"
 *       result:
 *         item: "mypack:cooked_fish"
 *         amount: 1            # optional, default: 1
 *       cook_time: 600         # optional, default: 600 ticks
 *       exp: 0.35              # optional, default: 0.0
 * }</pre>
 */
@NullMarked
public final class CampfireRecipeHandler extends AbstractRecipeHandler {
    public CampfireRecipeHandler() {
        super("CampfireRecipe");
    }

    @Override
    protected void registerRecipe(
            String namespace,
            String recipeId,
            ConfigurationSection entry,
            ItemStack ingredient,
            ItemStack result
    ) {
        int cookTime = entry.getInt("cook_time", 600);
        float exp = (float) entry.getDouble("exp", 0.0);

        NmsManager.instance()
                .handler()
                .campfireRecipes()
                .register(namespace, recipeId, ingredient, result, cookTime, exp);
    }

    @Override
    public void unregisterAll() {
        NmsManager.instance().handler().campfireRecipes().unregisterAll();
    }
}
