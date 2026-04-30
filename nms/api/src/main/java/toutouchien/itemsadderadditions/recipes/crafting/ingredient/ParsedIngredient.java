package toutouchien.itemsadderadditions.recipes.crafting.ingredient;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @param potionType Namespaced potion-type key (e.g. {@code "minecraft:infested"}).
 *                   {@code null} means no potion-type filtering.
 */
@NullMarked
public record ParsedIngredient(
        RecipeChoice choice, RecipeChoice validationChoice,
        int requiredAmount, int damageAmount,
        @Nullable ItemStack replacement,
        boolean ignoreDurability,
        @Nullable String potionType
) {
    /**
     * Convenience constructor - no potion type, no ignoreDurability.
     */
    public ParsedIngredient(
            RecipeChoice choice,
            int requiredAmount,
            int damageAmount,
            @Nullable ItemStack replacement
    ) {
        this(choice, choice, requiredAmount, damageAmount, replacement, false, null);
    }

    public boolean hasPredicate() {
        return requiredAmount > 1
                || damageAmount > 0
                || replacement != null
                || ignoreDurability
                || potionType != null;
    }
}
