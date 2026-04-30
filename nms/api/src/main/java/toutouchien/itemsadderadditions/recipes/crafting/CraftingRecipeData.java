// package toutouchien.itemsadderadditions.recipes.crafting;

package toutouchien.itemsadderadditions.recipes.crafting;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.recipes.crafting.ingredient.ParsedIngredient;

import java.util.Map;

/**
 * Immutable description of one crafting recipe registered by this plugin.
 *
 * @param key         Bukkit recipe key used for registration / lookup.
 * @param shaped      {@code true} for shaped, {@code false} for shapeless.
 * @param pattern     3-element string array (shaped only, {@code null} for shapeless).
 * @param ingredients Map from character key → parsed ingredient.
 * @param result      The item produced.
 * @param permission  Optional permission node required to use this recipe.
 */
@NullMarked
public record CraftingRecipeData(
        NamespacedKey key,
        boolean shaped,
        String @Nullable [] pattern,
        Map<Character, ParsedIngredient> ingredients,
        ItemStack result,
        @Nullable String permission
) {
    /**
     * Returns {@code true} when at least one ingredient has a predicate.
     */
    public boolean hasPredicates() {
        return ingredients.values().stream().anyMatch(ParsedIngredient::hasPredicate);
    }
}
