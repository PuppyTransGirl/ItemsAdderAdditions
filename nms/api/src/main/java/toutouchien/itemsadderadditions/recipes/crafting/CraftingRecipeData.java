package toutouchien.itemsadderadditions.recipes.crafting;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.recipes.crafting.ingredient.ParsedIngredient;

import java.util.*;

/**
 * Immutable description of one crafting recipe registered by this plugin.
 *
 * <p>Converted from a {@code record} to a {@code class} so that several
 * hot-path values can be <b>precomputed once</b> at construction time and
 * reused on every crafting event without repeated allocation or iteration.
 *
 * <h3>Precomputed caches</h3>
 * <ul>
 *   <li>{@link #hasPredicates} - replaces the per-event {@code stream().anyMatch()} call.</li>
 *   <li>{@link #ingredientList} - stable, index-addressable view of {@link #ingredients}
 *       values; eliminates repeated {@code List.copyOf(values())} in
 *       {@code applyPredicatesOnce}.</li>
 *   <li>{@link #materialIndex} - {@link Material} → candidate {@link ParsedIngredient}
 *       list; turns the O(matrix × ingredients) double-loop in
 *       {@code ingredientsSatisfied} into an O(matrix) single pass.</li>
 * </ul>
 */
@NullMarked
public final class CraftingRecipeData {
    /**
     * True when at least one ingredient has a predicate (amount, damage, etc.).
     */
    final boolean hasPredicates;
    /**
     * Stable, index-addressable list of ingredient values.
     * Eliminates repeated {@code List.copyOf(ingredients().values())} calls.
     */
    final List<ParsedIngredient> ingredientList;
    /**
     * Maps a {@link Material} to every {@link ParsedIngredient} whose
     * {@link ParsedIngredient#validationChoice()} can accept that material.
     * Used as a first-pass filter in {@code ingredientsSatisfied} so only
     * plausible ingredients are tested against each matrix slot.
     */
    final Map<Material, List<ParsedIngredient>> materialIndex;
    private final NamespacedKey key;
    private final boolean shaped;
    private final String @Nullable [] pattern;
    private final Map<Character, ParsedIngredient> ingredients;
    private final ItemStack result;
    private final @Nullable String permission;

    public CraftingRecipeData(
            NamespacedKey key,
            boolean shaped,
            String @Nullable [] pattern,
            Map<Character, ParsedIngredient> ingredients,
            ItemStack result,
            @Nullable String permission
    ) {
        this.key = key;
        this.shaped = shaped;
        this.pattern = pattern;
        this.ingredients = ingredients;
        this.result = result;
        this.permission = permission;

        // precompute hasPredicates
        boolean anyPredicate = false;
        for (ParsedIngredient ing : ingredients.values()) {
            if (ing.hasPredicate()) {
                anyPredicate = true;
                break;
            }
        }
        this.hasPredicates = anyPredicate;

        // precompute ingredientList
        List<ParsedIngredient> list = List.copyOf(ingredients.values());
        this.ingredientList = list;

        // precompute materialIndex
        this.materialIndex = buildMaterialIndex(list);
    }

    /**
     * Builds a {@link Material} → candidate-ingredient index from a list of
     * ingredients.
     *
     * <p>For each ingredient the set of possible materials is extracted from
     * its {@link ParsedIngredient#validationChoice()}:
     * <ul>
     *   <li>{@link RecipeChoice.MaterialChoice} - all materials in the tag.</li>
     *   <li>{@link RecipeChoice.ExactChoice} - the {@link Material} of every
     *       accepted {@link ItemStack}.</li>
     * </ul>
     */
    private static Map<Material, List<ParsedIngredient>> buildMaterialIndex(
            List<ParsedIngredient> ingredients
    ) {
        // Avoid resizing; typical recipe has ≤6 distinct ingredients
        Map<Material, List<ParsedIngredient>> index =
                new HashMap<>(ingredients.size() * 4);

        for (ParsedIngredient ing : ingredients) {
            for (Material mat : materialsOf(ing.validationChoice())) {
                index.computeIfAbsent(mat, k -> new ArrayList<>(2)).add(ing);
            }
        }
        return index;
    }

    /**
     * Returns every {@link Material} a {@link RecipeChoice} can accept.
     */
    private static Set<Material> materialsOf(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.MaterialChoice mc) {
            List<Material> choices = mc.getChoices();
            // Use EnumSet for O(1) contains + compact memory
            EnumSet<Material> set = EnumSet.noneOf(Material.class);
            set.addAll(choices);
            return set;
        }
        if (choice instanceof RecipeChoice.ExactChoice ec) {
            List<ItemStack> stacks = ec.getChoices();
            EnumSet<Material> set = EnumSet.noneOf(Material.class);
            for (ItemStack s : stacks) set.add(s.getType());
            return set;
        }
        return Set.of();
    }

    public NamespacedKey key() {
        return key;
    }

    public boolean shaped() {
        return shaped;
    }

    public String @Nullable [] pattern() {
        return pattern;
    }

    public Map<Character, ParsedIngredient> ingredients() {
        return ingredients;
    }

    public ItemStack result() {
        return result;
    }

    public @Nullable String permission() {
        return permission;
    }
}
