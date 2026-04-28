// package toutouchien.itemsadderadditions.recipes.crafting;

package toutouchien.itemsadderadditions.recipes.crafting;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.recipes.crafting.ingredient.IngredientResolver;
import toutouchien.itemsadderadditions.recipes.crafting.ingredient.ParsedIngredient;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NullMarked
public final class CraftingRecipeHandler {

    private static final String LOG_TAG = "CraftingRecipe";

    /**
     * All recipe keys we have registered with Bukkit (for clean unloading).
     */
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    /**
     * All recipes that carry at least one predicate.
     * Exposed to {@link CraftingRecipeListener} for post-craft processing.
     */
    private final List<CraftingRecipeData> predicateRecipes = new ArrayList<>();

    @Nullable
    private static ItemStack parseResult(
            String namespace, String recipeId, ConfigurationSection resultSec
    ) {
        String itemRef = resultSec.getString("item");
        if (itemRef == null) {
            Log.warn(LOG_TAG,
                    "Missing 'result.item' for {}:{}", namespace, recipeId);
            return null;
        }
        ItemStack item = NamespaceUtils.itemByID(namespace, itemRef);
        if (item == null) {
            Log.warn(LOG_TAG,
                    "Could not resolve result item '{}' for {}:{}",
                    itemRef, namespace, recipeId);
            return null;
        }
        item = item.clone();
        item.setAmount(resultSec.getInt("amount", 1));
        return item;
    }

    private static void registerShaped(CraftingRecipeData data) {
        ShapedRecipe recipe = new ShapedRecipe(data.key(), data.result());
        //noinspection ConstantConditions - pattern is non-null for shaped
        recipe.shape(data.pattern());
        data.ingredients().forEach((ch, ingredient) ->
                recipe.setIngredient(ch, ingredient.choice));
        Bukkit.addRecipe(recipe);
    }

    private static void registerShapeless(CraftingRecipeData data) {
        ShapelessRecipe recipe = new ShapelessRecipe(data.key(), data.result());
        data.ingredients().forEach((ch, ingredient) ->
                recipe.addIngredient(ingredient.choice));
        Bukkit.addRecipe(recipe);
    }

    public void load(String namespace, @Nullable ConfigurationSection section) {
        if (section == null) return;

        for (String recipeId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(recipeId);
            if (entry == null) continue;
            if (!entry.getBoolean("enabled", true)) continue;

            CraftingRecipeData data = parse(namespace, recipeId, entry);
            if (data == null) continue;

            register(data);
            Log.registered(LOG_TAG, namespace + ":" + recipeId);
        }
    }

    public void unregisterAll() {
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();
        predicateRecipes.clear();
    }

    /**
     * Used by {@link CraftingRecipeListener} to apply post-craft predicates.
     */
    public List<CraftingRecipeData> predicateRecipes() {
        return predicateRecipes;
    }

    @Nullable
    private CraftingRecipeData parse(
            String namespace, String recipeId, ConfigurationSection entry
    ) {
        // Result
        ConfigurationSection resultSec = entry.getConfigurationSection("result");
        if (resultSec == null) {
            Log.warn(LOG_TAG, "Missing 'result' for {}:{}", namespace, recipeId);
            return null;
        }
        ItemStack result = parseResult(namespace, recipeId, resultSec);
        if (result == null) return null;

        // Ingredients
        ConfigurationSection ingredientsSec =
                entry.getConfigurationSection("ingredients");
        if (ingredientsSec == null) {
            Log.warn(LOG_TAG,
                    "Missing 'ingredients' for {}:{}", namespace, recipeId);
            return null;
        }

        Map<Character, ParsedIngredient> ingredients = new HashMap<>();
        boolean anyFailed = false;
        for (String charKey : ingredientsSec.getKeys(false)) {
            if (charKey.length() != 1) {
                Log.warn(LOG_TAG,
                        "Ingredient key '{}' in {}:{} must be a single character.",
                        charKey, namespace, recipeId);
                anyFailed = true;
                continue;
            }
            ParsedIngredient ingredient = IngredientResolver.resolve(
                    namespace, ingredientsSec, charKey, recipeId);
            if (ingredient == null) {
                anyFailed = true;
            } else {
                ingredients.put(charKey.charAt(0), ingredient);
            }
        }
        if (anyFailed) return null;

        NamespacedKey key = new NamespacedKey(
                "iaa", namespace + "_" + recipeId.toLowerCase());
        String permission = entry.getString("permission", null);
        boolean shapeless = entry.getBoolean("shapeless", false);

        if (shapeless) {
            return new CraftingRecipeData(
                    key, false, null, ingredients, result, permission);
        }

        // Shaped: collect pattern lines
        List<String> patternList = entry.getStringList("pattern");
        if (patternList.isEmpty()) {
            Log.warn(LOG_TAG,
                    "Shaped recipe {}:{} is missing 'pattern'.", namespace, recipeId);
            return null;
        }
        String[] pattern = patternList.toArray(new String[0]);
        return new CraftingRecipeData(
                key, true, pattern, ingredients, result, permission);
    }

    private void register(CraftingRecipeData data) {
        try {
            if (data.shaped()) {
                registerShaped(data);
            } else {
                registerShapeless(data);
            }
            registeredKeys.add(data.key());
            if (data.hasPredicates()) {
                predicateRecipes.add(data);
            }
        } catch (Exception e) {
            Log.error(LOG_TAG, "Failed to register recipe '{}'", data.key(), e);
        }
    }
}
