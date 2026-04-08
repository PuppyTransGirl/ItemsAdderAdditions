package toutouchien.itemsadderadditions.recipes.campfire;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

/**
 * Immutable data bag for a single campfire cooking recipe entry.
 */
public record CampfireRecipeData(
        String namespace,
        String id,
        boolean enabled,
        String permission,
        RecipeChoice ingredient,
        ItemStack result,
        int cookTime,
        float exp
) {}
