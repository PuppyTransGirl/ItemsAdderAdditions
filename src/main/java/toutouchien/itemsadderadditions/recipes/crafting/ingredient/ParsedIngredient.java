package toutouchien.itemsadderadditions.recipes.crafting.ingredient;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One parsed ingredient slot, combining a {@link RecipeChoice} (what item/tag
 * is accepted) with optional predicates applied after the craft occurs.
 *
 * <ul>
 *   <li><b>requiredAmount</b> - the ingredient stack must contain at least
 *       this many items; the extra items above 1 are consumed.</li>
 *   <li><b>damageAmount</b>   - durability damage applied to the ingredient
 *       item after the craft; incompatible with replacement.</li>
 *   <li><b>replacement</b>   - item placed back into the ingredient slot after
 *       the craft; incompatible with damage.</li>
 *   <li><b>ignoreDurability</b> - when {@code true}, the listener strips the
 *       item's current damage before comparing against {@link #validationChoice},
 *       allowing any-durability items to satisfy this ingredient.</li>
 * </ul>
 */
@NullMarked
public final class ParsedIngredient {

    /**
     * Choice registered with Bukkit.
     * For custom items with {@link #ignoreDurability} this is a
     * {@link RecipeChoice.MaterialChoice} so Bukkit shows the recipe result
     * regardless of durability. For all other cases it equals
     * {@link #validationChoice}.
     */
    public final RecipeChoice choice;

    /**
     * Choice used by the listener for slot validation.
     * For custom items with {@link #ignoreDurability} this keeps the original
     * {@link RecipeChoice.ExactChoice} so that vanilla items of the same
     * material are still rejected; the listener strips damage before testing.
     * For all other cases this equals {@link #choice}.
     */
    public final RecipeChoice validationChoice;

    /**
     * Minimum stack size the slot must hold. Defaults to 1.
     * Items above 1 are consumed during the craft.
     */
    public final int requiredAmount;

    /**
     * Durability points to subtract from the ingredient after the craft.
     * 0 means no damage. Mutually exclusive with {@link #replacement}.
     */
    public final int damageAmount;

    /**
     * Item placed back into the slot after the craft.
     * {@code null} means the slot is cleared normally.
     * Mutually exclusive with {@link #damageAmount}.
     */
    @Nullable
    public final ItemStack replacement;

    /**
     * When {@code true} the listener ignores the current durability of the
     * slot item when matching against {@link #validationChoice}.
     */
    public final boolean ignoreDurability;

    public ParsedIngredient(
            RecipeChoice choice,
            RecipeChoice validationChoice,
            int requiredAmount,
            int damageAmount,
            @Nullable ItemStack replacement,
            boolean ignoreDurability
    ) {
        this.choice = choice;
        this.validationChoice = validationChoice;
        this.requiredAmount = requiredAmount;
        this.damageAmount = damageAmount;
        this.replacement = replacement;
        this.ignoreDurability = ignoreDurability;
    }

    /**
     * Convenience constructor when {@code validationChoice == choice}
     * and {@code ignoreDurability == false}.
     */
    public ParsedIngredient(
            RecipeChoice choice,
            int requiredAmount,
            int damageAmount,
            @Nullable ItemStack replacement
    ) {
        this(choice, choice, requiredAmount, damageAmount, replacement, false);
    }

    /**
     * Returns {@code true} if this ingredient requires any post-craft behaviour
     * or listener-side validation that Bukkit cannot enforce on its own.
     */
    public boolean hasPredicate() {
        return requiredAmount > 1
                || damageAmount > 0
                || replacement != null
                || ignoreDurability; // listener must validate custom item vs. MaterialChoice
    }
}
