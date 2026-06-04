package toutouchien.itemsadderadditions.feature.recipe.crafting;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentsManager;
import toutouchien.itemsadderadditions.feature.recipe.RecipeActions;
import toutouchien.itemsadderadditions.feature.recipe.RecipeActionsParser;
import toutouchien.itemsadderadditions.feature.recipe.crafting.ingredient.IngredientResolver;
import toutouchien.itemsadderadditions.feature.recipe.crafting.ingredient.ParsedIngredient;
import toutouchien.itemsadderadditions.nms.api.INmsCraftingRecipeHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NullMarked
public final class CraftingRecipeHandler {
    private static final String LOG_TAG = "CraftingRecipe";

    private final INmsCraftingRecipeHandler nms;
    private final @Nullable ComponentsManager componentsManager;
    private final List<CraftingRecipeData> predicateRecipes = new ArrayList<>();
    /**
     * O(1) lookup used by {@code CraftingRecipeListener.matchRecipe()}.
     *
     * <p>Previously {@code matchRecipe} iterated the full {@link #predicateRecipes}
     * list on every {@link org.bukkit.event.inventory.PrepareItemCraftEvent},
     * {@link org.bukkit.event.inventory.CraftItemEvent}, etc.  With even a
     * modest number of registered recipes this was the dominant per-event cost.
     * This map replaces that O(n) scan with an O(1) hash lookup.
     */
    private final Map<NamespacedKey, CraftingRecipeData> predicateByKey = new HashMap<>();
    private final Map<NamespacedKey, RecipeActions> actionsMap = new HashMap<>();
    private int loadedCount = 0;

    public CraftingRecipeHandler(INmsCraftingRecipeHandler nms) {
        this(nms, null);
    }

    public CraftingRecipeHandler(INmsCraftingRecipeHandler nms, @Nullable ComponentsManager componentsManager) {
        this.nms = nms;
        this.componentsManager = componentsManager;
    }

    /**
     * Resolves the {@code ingredients} node, which may be either a
     * char-keyed {@link ConfigurationSection} or a raw list (anonymous
     * shapeless form).
     */
    @Nullable
    private static Map<Character, ParsedIngredient> parseIngredients(
            String namespace, String recipeId, ConfigurationSection entry
    ) {
        List<?> rawList = entry.getList("ingredients");
        if (rawList != null) {
            return IngredientResolver.resolveList(namespace, rawList, recipeId);
        }

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

        return anyFailed ? null : ingredients;
    }

    /**
     * Collects all pattern variants from the entry.
     *
     * <p>A "pattern key" is any key whose name starts with {@code "pattern"}
     * (case-insensitive).
     */
    private static List<String[]> collectPatterns(
            String namespace,
            String recipeId,
            ConfigurationSection entry,
            Map<Character, ParsedIngredient> ingredients
    ) {
        List<String[]> result = new ArrayList<>();

        for (String key : entry.getKeys(false)) {
            if (!key.toLowerCase().startsWith("pattern")) continue;

            List<String> lines = entry.getStringList(key);
            if (lines.isEmpty()) {
                Log.warn(LOG_TAG,
                        "Pattern '{}' in {}:{} is empty or not a list - skipping.",
                        key, namespace, recipeId);
                continue;
            }

            result.add(normalisePattern(lines, ingredients));
        }

        if (result.isEmpty()) {
            Log.warn(LOG_TAG,
                    "Shaped recipe {}:{} has no valid 'pattern' key.", namespace, recipeId);
        }

        return result;
    }

    /**
     * Replaces any character not present in {@code ingredients} with a space.
     */
    private static String[] normalisePattern(
            List<String> lines,
            Map<Character, ParsedIngredient> ingredients
    ) {
        String[] pattern = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            StringBuilder sb = new StringBuilder(line.length());
            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);
                sb.append((c == ' ' || ingredients.containsKey(c)) ? c : ' ');
            }
            pattern[i] = sb.toString();
        }
        return pattern;
    }

    private static NamespacedKey recipeKey(
            String namespace, String recipeId, String suffix
    ) {
        return new NamespacedKey(namespace, (recipeId + suffix).toLowerCase());
    }

    @Nullable
    private ItemStack parseResult(
            String namespace, String recipeId, ConfigurationSection resultSec
    ) {
        ConfigurationSection actualResultSec = unwrapResultSection(namespace, recipeId, resultSec);
        if (actualResultSec == null) return null;

        String itemRef = actualResultSec.getString("item");
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
        item = applyResultComponents(namespace, recipeId, item, actualResultSec);
        item.setAmount(actualResultSec.getInt("amount", 1));
        return item;
    }

    @Nullable
    private static ConfigurationSection unwrapResultSection(
            String namespace, String recipeId, ConfigurationSection resultSec
    ) {
        if (resultSec.isString("item")) return resultSec;

        ConfigurationSection nested = null;
        for (String key : resultSec.getKeys(false)) {
            ConfigurationSection child = resultSec.getConfigurationSection(key);
            if (child == null || !child.isString("item")) continue;
            if (nested != null) {
                Log.warn(LOG_TAG,
                        "Result for {}:{} has multiple nested item sections; expected exactly one.",
                        namespace, recipeId);
                return null;
            }
            nested = child;
        }

        if (nested != null) return nested;

        Log.warn(LOG_TAG, "Missing 'result.item' for {}:{}", namespace, recipeId);
        return null;
    }

    private ItemStack applyResultComponents(
            String namespace,
            String recipeId,
            ItemStack item,
            ConfigurationSection resultSec
    ) {
        if (!resultSec.contains("components")) return item;

        ConfigurationSection componentsSection = resultSec.getConfigurationSection("components");
        if (componentsSection == null) {
            Log.warn(LOG_TAG,
                    "'result.components' for {}:{} must be a section/map - skipping components.",
                    namespace, recipeId);
            return item;
        }

        if (componentsManager == null) {
            Log.warn(LOG_TAG,
                    "'result.components' for {}:{} cannot be applied because the component system is unavailable.",
                    namespace, recipeId);
            return item;
        }

        return componentsManager.applyComponentsToStack(
                item,
                componentsSection,
                namespace + ":" + recipeId + " result"
        );
    }

    public List<CraftingRecipeData> predicateRecipes() {
        return predicateRecipes;
    }

    /**
     * Returns the number of crafting recipes successfully registered since the last reload.
     */
    public int loadedCount() {
        return loadedCount;
    }

    /**
     * Resets the counter. Called by {@link toutouchien.itemsadderadditions.feature.recipe.RecipeLoader} before each reload.
     */
    public void resetCount() {
        loadedCount = 0;
    }

    /**
     * O(1) recipe lookup by {@link NamespacedKey}.
     * Used by {@code CraftingRecipeListener} instead of the old linear scan.
     */
    @Nullable
    public CraftingRecipeData predicateRecipeByKey(NamespacedKey key) {
        return predicateByKey.get(key);
    }

    /**
     * Returns the {@link RecipeActions} registered for this recipe key, or
     * {@link RecipeActions#EMPTY} when none were configured.
     */
    public RecipeActions actionsFor(NamespacedKey key) {
        return actionsMap.getOrDefault(key, RecipeActions.EMPTY);
    }

    public void load(String namespace, @Nullable ConfigurationSection section) {
        if (section == null) return;

        for (String recipeId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(recipeId);
            if (entry == null) continue;
            if (!entry.getBoolean("enabled", true)) continue;

            List<CraftingRecipeData> variants = parse(namespace, recipeId, entry);
            RecipeActions actions = RecipeActionsParser.parse(entry.getConfigurationSection("on_complete"));
            for (CraftingRecipeData data : variants) {
                register(data, actions);
                Log.debug(LOG_TAG, "Registered: " + data.key());
            }
        }
    }

    public void unregisterAll() {
        nms.unregisterAll();
        predicateRecipes.clear();
        predicateByKey.clear();
        actionsMap.clear();
        loadedCount = 0;
    }

    /**
     * Parses one recipe entry and returns one {@link CraftingRecipeData} per
     * pattern variant. Returns an empty list on any parse error.
     */
    private List<CraftingRecipeData> parse(
            String namespace, String recipeId, ConfigurationSection entry
    ) {
        ConfigurationSection resultSec = entry.getConfigurationSection("result");
        if (resultSec == null) {
            Log.warn(LOG_TAG, "Missing 'result' for {}:{}", namespace, recipeId);
            return List.of();
        }
        ItemStack result = parseResult(namespace, recipeId, resultSec);
        if (result == null) return List.of();

        String permission = entry.getString("permission", null);
        boolean shapeless = entry.getBoolean("shapeless", false);

        Map<Character, ParsedIngredient> ingredients =
                parseIngredients(namespace, recipeId, entry);
        if (ingredients == null) return List.of();

        if (shapeless) {
            NamespacedKey key = recipeKey(namespace, recipeId, "");
            return List.of(new CraftingRecipeData(
                    key, false, null, ingredients, result, permission));
        }

        List<String[]> patterns = collectPatterns(namespace, recipeId, entry, ingredients);
        if (patterns.isEmpty()) return List.of();

        List<CraftingRecipeData> variants = new ArrayList<>(patterns.size());
        for (int i = 0; i < patterns.size(); i++) {
            String suffix = (i == 0) ? "" : "_v" + (i + 1);
            NamespacedKey key = recipeKey(namespace, recipeId, suffix);
            variants.add(new CraftingRecipeData(
                    key, true, patterns.get(i), ingredients, result, permission));
        }
        return variants;
    }

    private void register(CraftingRecipeData data, RecipeActions actions) {
        try {
            nms.register(data);
            loadedCount++;
            if (data.hasPredicates) {
                predicateRecipes.add(data);
                predicateByKey.put(data.key(), data);
            }
            if (!actions.isEmpty()) {
                actionsMap.put(data.key(), actions);
            }
        } catch (Exception e) {
            Log.error(LOG_TAG, "Failed to register recipe " + data.key(), e);
        }
    }
}
