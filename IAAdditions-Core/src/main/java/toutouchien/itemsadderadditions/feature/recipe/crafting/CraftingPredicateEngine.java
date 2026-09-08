package toutouchien.itemsadderadditions.feature.recipe.crafting;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.recipe.crafting.ingredient.ParsedIngredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Stateless crafting predicate facade used by {@link CraftingRecipeListener}.
 *
 * <p>Event flow stays in the listener; matching, consumption and result delivery
 * stay here. Ingredient-specific checks are delegated to
 * {@link CraftingIngredientMatcher} so the matrix algorithms are readable.</p>
 */
@NullMarked
final class CraftingPredicateEngine {
    private CraftingPredicateEngine() {
    }

    static boolean ingredientsSatisfied(CraftingRecipeData data, ItemStack[] matrix) {
        int[] totals = countMatches(data, matrix, ingredient -> true);
        for (int i = 0; i < data.ingredientList.size(); i++) {
            if (totals[i] < data.ingredientList.get(i).requiredAmount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether two recipes are indistinguishable to the native recipe
     * matcher. Runtime-only predicates such as custom item IDs, custom tags,
     * amounts and durability are deliberately ignored here because the NMS
     * registration contains only each ingredient's broad {@link
     * ParsedIngredient#choice()}.
     */
    static boolean hasEquivalentRegistrationShape(
            CraftingRecipeData first,
            CraftingRecipeData second
    ) {
        if (first.shaped() != second.shaped()) return false;

        if (!first.shaped()) {
            return unorderedChoicesEquivalent(first.ingredientList, second.ingredientList);
        }

        RegistrationPattern firstPattern = registrationPattern(first);
        RegistrationPattern secondPattern = registrationPattern(second);
        if (firstPattern.width != secondPattern.width
                || firstPattern.height != secondPattern.height) {
            return false;
        }

        return patternsEquivalent(firstPattern, secondPattern, false)
                || patternsEquivalent(firstPattern, secondPattern, true);
    }

    static boolean canCraftAgain(CraftingRecipeData data, ItemStack[] matrix) {
        int[] totals = countMatches(data, matrix, CraftingPredicateEngine::isConsumedIngredient);
        for (int i = 0; i < data.ingredientList.size(); i++) {
            ParsedIngredient ingredient = data.ingredientList.get(i);
            if (isConsumedIngredient(ingredient) && totals[i] < ingredient.requiredAmount()) {
                return false;
            }
        }
        return true;
    }

    static void applyPredicatesOnce(CraftingRecipeData data, ItemStack[] matrix) {
        List<ParsedIngredient> ingredients = data.ingredientList;
        int[] remaining = ingredients.stream().mapToInt(ParsedIngredient::requiredAmount).toArray();
        int[] slotToIngredient = mapSlotsToIngredients(data, matrix);

        for (int slotIndex = 0; slotIndex < matrix.length; slotIndex++) {
            ItemStack slot = matrix[slotIndex];
            if (isAir(slot)) continue;

            int ingredientIndex = slotToIngredient[slotIndex];
            if (ingredientIndex < 0) continue;

            ParsedIngredient ingredient = ingredients.get(ingredientIndex);
            matrix[slotIndex] = mutateSlot(slot, ingredient, remaining, ingredientIndex);
        }
    }

    @Nullable
    static ParsedIngredient findIngredient(CraftingRecipeData data, ItemStack slot) {
        List<ParsedIngredient> candidates = data.materialIndex.get(slot.getType());
        if (candidates == null) return null;

        for (ParsedIngredient ingredient : candidates) {
            if (testIngredient(ingredient, slot)) return ingredient;
        }
        return null;
    }

    static boolean testIngredient(ParsedIngredient ingredient, ItemStack slot) {
        return CraftingIngredientMatcher.matches(ingredient, slot);
    }

    static int calculateMaxCrafts(CraftingRecipeData data, ItemStack[] matrix, HumanEntity player) {
        int maxCraftsFromIngredients = Integer.MAX_VALUE;

        for (ParsedIngredient ingredient : data.ingredientList) {
            if (ingredient.replacement() != null) {
                maxCraftsFromIngredients = Math.min(maxCraftsFromIngredients, 1);
                continue;
            }

            int possible = ingredient.damageAmount() > 0
                    ? maxCraftsFromDurability(ingredient, matrix)
                    : maxCraftsFromAmount(ingredient, matrix);
            maxCraftsFromIngredients = Math.min(maxCraftsFromIngredients, possible);
        }

        int ingredientLimit = maxCraftsFromIngredients == Integer.MAX_VALUE || maxCraftsFromIngredients <= 0
                ? 1
                : maxCraftsFromIngredients;
        int maxNeededSpace = data.result().getAmount() * ingredientLimit;
        int inventoryLimit = countInventorySpace(player, data.result(), maxNeededSpace) / data.result().getAmount();
        return Math.max(1, Math.min(ingredientLimit, inventoryLimit));
    }

    static void giveOrDrop(HumanEntity player, ItemStack item) {
        player.getInventory().addItem(item.clone())
                .values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    static boolean applyDamage(ItemStack item, int damage) {
        return CraftingIngredientMatcher.applyDamage(item, damage);
    }

    static int countInventorySpace(HumanEntity player, ItemStack template, int maxNeeded) {
        int space = 0;
        int maxStack = template.getMaxStackSize();
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (isAir(slot)) {
                space += maxStack;
            } else if (slot.isSimilar(template)) {
                space += maxStack - slot.getAmount();
            }
            if (space >= maxNeeded) return space;
        }
        return space;
    }

    @Nullable
    static NamespacedKey recipeKey(Recipe recipe) {
        return recipe instanceof Keyed keyed ? keyed.getKey() : null;
    }

    static int getRemainingDurability(ItemStack item) {
        return CraftingIngredientMatcher.remainingDurability(item);
    }

    static boolean isAir(@Nullable ItemStack item) {
        return CraftingIngredientMatcher.isAir(item);
    }

    static String itemInfo(@Nullable ItemStack item) {
        return CraftingIngredientMatcher.itemInfo(item);
    }

    private static boolean unorderedChoicesEquivalent(
            List<ParsedIngredient> first,
            List<ParsedIngredient> second
    ) {
        if (first.size() != second.size()) return false;
        return matchUnorderedChoice(first, second, new boolean[second.size()], 0);
    }

    private static boolean matchUnorderedChoice(
            List<ParsedIngredient> first,
            List<ParsedIngredient> second,
            boolean[] used,
            int firstIndex
    ) {
        if (firstIndex == first.size()) return true;

        RecipeChoice choice = first.get(firstIndex).choice();
        for (int secondIndex = 0; secondIndex < second.size(); secondIndex++) {
            if (used[secondIndex]
                    || !choicesEquivalent(choice, second.get(secondIndex).choice())) {
                continue;
            }

            used[secondIndex] = true;
            if (matchUnorderedChoice(first, second, used, firstIndex + 1)) return true;
            used[secondIndex] = false;
        }
        return false;
    }

    private static RegistrationPattern registrationPattern(CraftingRecipeData data) {
        String[] pattern = data.pattern();
        assert pattern != null;

        int minRow = pattern.length;
        int maxRow = -1;
        int minColumn = Integer.MAX_VALUE;
        int maxColumn = -1;

        for (int row = 0; row < pattern.length; row++) {
            String line = pattern[row];
            for (int column = 0; column < line.length(); column++) {
                if (line.charAt(column) == ' ') continue;
                minRow = Math.min(minRow, row);
                maxRow = Math.max(maxRow, row);
                minColumn = Math.min(minColumn, column);
                maxColumn = Math.max(maxColumn, column);
            }
        }

        if (maxRow < 0) return new RegistrationPattern(0, 0, new ParsedIngredient[0]);

        int width = maxColumn - minColumn + 1;
        int height = maxRow - minRow + 1;
        ParsedIngredient[] cells = new ParsedIngredient[width * height];
        for (int row = 0; row < height; row++) {
            String line = pattern[minRow + row];
            for (int column = 0; column < width; column++) {
                int sourceColumn = minColumn + column;
                char key = sourceColumn < line.length() ? line.charAt(sourceColumn) : ' ';
                if (key != ' ') {
                    cells[row * width + column] = data.ingredients().get(key);
                }
            }
        }
        return new RegistrationPattern(width, height, cells);
    }

    private static boolean patternsEquivalent(
            RegistrationPattern first,
            RegistrationPattern second,
            boolean mirrored
    ) {
        for (int row = 0; row < first.height; row++) {
            for (int column = 0; column < first.width; column++) {
                ParsedIngredient firstIngredient = first.at(row, column);
                int secondColumn = mirrored ? second.width - column - 1 : column;
                ParsedIngredient secondIngredient = second.at(row, secondColumn);
                if (firstIngredient == null || secondIngredient == null) {
                    if (firstIngredient != secondIngredient) return false;
                } else if (!choicesEquivalent(
                        firstIngredient.choice(), secondIngredient.choice())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean choicesEquivalent(RecipeChoice first, RecipeChoice second) {
        if (first instanceof RecipeChoice.MaterialChoice firstMaterials
                && second instanceof RecipeChoice.MaterialChoice secondMaterials) {
            return materialSet(firstMaterials).equals(materialSet(secondMaterials));
        }

        if (first instanceof RecipeChoice.ExactChoice firstExact
                && second instanceof RecipeChoice.ExactChoice secondExact) {
            List<ItemStack> unmatched = new ArrayList<>(secondExact.getChoices());
            for (ItemStack expected : firstExact.getChoices()) {
                int match = -1;
                for (int i = 0; i < unmatched.size(); i++) {
                    if (expected.isSimilar(unmatched.get(i))) {
                        match = i;
                        break;
                    }
                }
                if (match < 0) return false;
                unmatched.remove(match);
            }
            return unmatched.isEmpty();
        }

        return first.equals(second);
    }

    private static EnumSet<Material> materialSet(RecipeChoice.MaterialChoice choice) {
        EnumSet<Material> materials = EnumSet.noneOf(Material.class);
        materials.addAll(choice.getChoices());
        return materials;
    }

    @Nullable
    static ItemStack[] toNineSlot(ItemStack[] matrix) {
        if (matrix.length == 9) return matrix;
        if (matrix.length != 4) return null;

        return new ItemStack[]{
                matrix[0], matrix[1], null,
                matrix[2], matrix[3], null,
                null, null, null
        };
    }

    private static int[] countMatches(
            CraftingRecipeData data,
            ItemStack[] matrix,
            IngredientFilter filter
    ) {
        int[] totals = new int[data.ingredientList.size()];
        for (ItemStack slot : matrix) {
            if (isAir(slot)) continue;

            ParsedIngredient ingredient = firstMatchingIngredient(data.materialIndex, slot, filter);
            if (ingredient != null) {
                int index = identityIndexOf(data.ingredientList, ingredient);
                if (index >= 0) {
                    totals[index] += slot.getAmount();
                }
            }
        }
        return totals;
    }

    private static int[] mapSlotsToIngredients(CraftingRecipeData data, ItemStack[] matrix) {
        int[] mapping = new int[matrix.length];
        Arrays.fill(mapping, -1);

        for (int slotIndex = 0; slotIndex < matrix.length; slotIndex++) {
            ItemStack slot = matrix[slotIndex];
            if (isAir(slot)) continue;

            ParsedIngredient ingredient = firstMatchingIngredient(data.materialIndex, slot, i -> true);
            if (ingredient != null) {
                mapping[slotIndex] = identityIndexOf(data.ingredientList, ingredient);
            }
        }
        return mapping;
    }

    @Nullable
    private static ParsedIngredient firstMatchingIngredient(
            Map<Material, List<ParsedIngredient>> materialIndex,
            ItemStack slot,
            IngredientFilter filter
    ) {
        List<ParsedIngredient> candidates = materialIndex.get(slot.getType());
        if (candidates == null) return null;

        for (ParsedIngredient ingredient : candidates) {
            if (filter.accepts(ingredient) && testIngredient(ingredient, slot)) {
                return ingredient;
            }
        }
        return null;
    }

    @Nullable
    private static ItemStack mutateSlot(
            ItemStack slot,
            ParsedIngredient ingredient,
            int[] remaining,
            int ingredientIndex
    ) {
        if (ingredient.replacement() != null) {
            return ingredient.replacement().clone();
        }
        if (ingredient.damageAmount() > 0) {
            ItemStack damaged = slot.clone();
            return applyDamage(damaged, ingredient.damageAmount()) ? null : damaged;
        }

        int needed = remaining[ingredientIndex];
        if (needed <= 0) return slot;

        int consumed = Math.min(slot.getAmount(), needed);
        int leftover = slot.getAmount() - consumed;
        remaining[ingredientIndex] = needed - consumed;

        if (leftover <= 0) return null;

        ItemStack result = slot.clone();
        result.setAmount(leftover);
        return result;
    }

    private static int maxCraftsFromAmount(ParsedIngredient ingredient, ItemStack[] matrix) {
        int totalAmount = 0;
        for (ItemStack slot : matrix) {
            if (!isAir(slot) && testIngredient(ingredient, slot)) {
                totalAmount += slot.getAmount();
            }
        }
        return totalAmount / Math.max(1, ingredient.requiredAmount());
    }

    private static int maxCraftsFromDurability(ParsedIngredient ingredient, ItemStack[] matrix) {
        int min = Integer.MAX_VALUE;
        for (ItemStack slot : matrix) {
            if (isAir(slot) || !testIngredient(ingredient, slot)) continue;

            int durability = getRemainingDurability(slot);
            int da = ingredient.damageAmount();
            // Ceiling division: the last craft may over-damage and break the item,
            // which applyDamage handles correctly.
            int crafts = durability > 0 ? (durability + da - 1) / da : 1;
            min = Math.min(min, crafts);
        }
        return min;
    }

    private static boolean isConsumedIngredient(ParsedIngredient ingredient) {
        return ingredient.replacement() == null && ingredient.damageAmount() <= 0;
    }

    private static int identityIndexOf(List<ParsedIngredient> list, ParsedIngredient target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == target) return i;
        }
        Log.warn("Crafting", "Matched ingredient was not present in recipe cache: {}", target);
        return -1;
    }

    @FunctionalInterface
    private interface IngredientFilter {
        boolean accepts(ParsedIngredient ingredient);
    }

    private record RegistrationPattern(
            int width,
            int height,
            ParsedIngredient @Nullable [] cells
    ) {
        @Nullable
        ParsedIngredient at(int row, int column) {
            return cells[row * width + column];
        }
    }
}
