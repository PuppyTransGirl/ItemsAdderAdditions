package toutouchien.itemsadderadditions.feature.recipe.crafting.ingredient;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @param choice             Registration choice passed to Bukkit's recipe API.
 * @param validationChoice   Choice used to validate matrix slots at craft time.
 *                           Differs from {@code choice} only when
 *                           {@code ignoreDurability} is set (registration uses
 *                           {@link RecipeChoice.MaterialChoice}; validation keeps
 *                           the original {@link RecipeChoice.ExactChoice} so the
 *                           custom-item identity is still checked).
 * @param requiredAmount     Minimum total amount of this ingredient required.
 * @param damageAmount       Durability damage applied to the ingredient on craft.
 * @param replacement        If non-null, replaces the slot instead of consuming it.
 * @param ignoreDurability   Whether to ignore current durability when matching.
 * @param potionType         Namespaced potion-type key (e.g. {@code "minecraft:infested"}).
 *                           {@code null} means no potion-type filtering.
 * @param customNamespacedId The ItemsAdder namespaced ID of this ingredient
 *                           (e.g. {@code "mypack:my_sword"}), or {@code null}
 *                           for vanilla ingredients. When non-null, validation
 *                           uses a fast {@link dev.lone.itemsadder.api.CustomStack}
 *                           ID check instead of the expensive
 *                           {@link ItemStack#isSimilar} path inside
 *                           {@link RecipeChoice.ExactChoice#test}.
 * @param customTagId        The normalized ID (no {@code '#'}) of an
 *                           ItemsAdderAdditions custom ITEM tag, or {@code null}
 *                           when this ingredient is not a custom tag. When
 *                           non-null, validation matches the slot against tag
 *                           membership at craft time instead of the registration
 *                           {@link RecipeChoice}, so only items actually in the
 *                           tag are accepted.
 */
@NullMarked
public record ParsedIngredient(
        RecipeChoice choice,
        RecipeChoice validationChoice,
        int requiredAmount,
        int damageAmount,
        @Nullable ItemStack replacement,
        boolean ignoreDurability,
        @Nullable String potionType,
        @Nullable String customNamespacedId,
        int customNamespacedIdHash,       // 0 when customNamespacedId is null
        @Nullable String customTagId
) {
    /**
     * Compact constructor - derives {@link #customNamespacedIdHash} from
     * {@link #customNamespacedId} so callers never supply the hash manually.
     * Java {@link String#hashCode()} caches the result after the first call,
     * so subsequent reads are free integer loads.
     */
    public ParsedIngredient {
        customNamespacedIdHash = customNamespacedId != null
                ? customNamespacedId.hashCode()
                : 0;
    }

    /**
     * Back-compat constructor for ingredients that are not custom tags.
     */
    public ParsedIngredient(
            RecipeChoice choice,
            RecipeChoice validationChoice,
            int requiredAmount,
            int damageAmount,
            @Nullable ItemStack replacement,
            boolean ignoreDurability,
            @Nullable String potionType,
            @Nullable String customNamespacedId,
            int customNamespacedIdHash
    ) {
        this(choice, validationChoice, requiredAmount, damageAmount, replacement,
                ignoreDurability, potionType, customNamespacedId, customNamespacedIdHash, null);
    }

    /**
     * Convenience constructor for vanilla ingredients (no custom-item fields).
     */
    public ParsedIngredient(
            RecipeChoice choice,
            int requiredAmount,
            int damageAmount,
            @Nullable ItemStack replacement
    ) {
        this(choice, choice, requiredAmount, damageAmount, replacement,
                false, null, null, 0, null);
    }

    /**
     * Returns {@code true} when this ingredient is an ItemsAdder custom item.
     */
    public boolean isCustomItem() {
        return customNamespacedId != null;
    }

    /**
     * Returns {@code true} when this ingredient is an ItemsAdderAdditions custom
     * ITEM tag.
     */
    public boolean isCustomTag() {
        return customTagId != null;
    }

    public boolean hasPredicate() {
        return isCustomItem()       // custom items always need listener-level identity check
                || isCustomTag()    // custom tags need listener-level membership check
                || requiredAmount > 1
                || damageAmount > 0
                || replacement != null
                || ignoreDurability
                || potionType != null;
    }
}
