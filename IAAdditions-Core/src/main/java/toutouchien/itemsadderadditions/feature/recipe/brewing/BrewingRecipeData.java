package toutouchien.itemsadderadditions.feature.recipe.brewing;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.recipe.RecipeActions;

@NullMarked
public record BrewingRecipeData(
        String key,
        ItemStack base,
        ItemStack ingredient,
        int ingredientConsume,
        ItemStack result,
        int brewTime,
        int fuelCost,
        RecipeActions actions
) {
}
