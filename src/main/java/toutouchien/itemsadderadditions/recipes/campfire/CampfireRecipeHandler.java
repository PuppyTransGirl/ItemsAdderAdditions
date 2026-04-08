package toutouchien.itemsadderadditions.recipes.campfire;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.recipes.RecipeItemResolver;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the {@code recipes.campfire_cooking} YAML section and registers
 * {@link org.bukkit.inventory.CampfireRecipe} entries into the server.
 */
public class CampfireRecipeHandler {
    private static final String LOG_TAG = "CampfireRecipe";
    /**
     * Key prefix used for all NamespacedKeys we register, to allow clean removal.
     */
    public static final String KEY_PREFIX = "iaa_campfire_";

    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    /**
     * Parses {@code campfire_cooking} sub-section of a namespace YAML file.
     *
     * @param namespace The IA namespace (e.g. {@code myitems})
     * @param section   The {@code recipes.campfire_cooking} ConfigurationSection
     */
    public void load(String namespace, ConfigurationSection section) {
        if (section == null) return;

        for (String recipeId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(recipeId);
            if (entry == null) continue;

            boolean enabled = entry.getBoolean("enabled", true);
            if (!enabled) continue;

            ConfigurationSection ingredientSection = entry.getConfigurationSection("ingredient");
            if (ingredientSection == null) {
                Log.warn(LOG_TAG, "Missing 'ingredient' for " + namespace + ":" + recipeId);
                continue;
            }
            String ingredientValue = ingredientSection.getString("item");
            RecipeChoice ingredient = RecipeItemResolver.resolve(ingredientValue, namespace, LOG_TAG);
            if (ingredient == null) continue;

            ConfigurationSection resultSection = entry.getConfigurationSection("result");
            if (resultSection == null) {
                Log.warn(LOG_TAG, "Missing 'result' for " + namespace + ":" + recipeId);
                continue;
            }
            ItemStack result = resolveResult(resultSection, namespace, recipeId);
            if (result == null) continue;

            int cookTime = entry.getInt("cook_time", 600);
            float exp = (float) entry.getDouble("exp", 0.0);
            String permission = entry.getString("permission", null);

            register(namespace, recipeId, ingredient, result, cookTime, exp, permission);
        }
    }

    private void register(
            String namespace,
            String recipeId,
            RecipeChoice ingredient,
            ItemStack result,
            int cookTime,
            float exp,
            String permission
    ) {
        NamespacedKey key = new NamespacedKey(
                ItemsAdderAdditions.instance(),
                KEY_PREFIX + namespace + "_" + recipeId
        );

        // Remove if already registered (e.g. after reload)
        Bukkit.removeRecipe(key);

        org.bukkit.inventory.CampfireRecipe recipe =
                new org.bukkit.inventory.CampfireRecipe(key, result, ingredient, exp, cookTime);

        Bukkit.addRecipe(recipe);
        registeredKeys.add(key);

        Log.info(LOG_TAG, "Registered campfire recipe: " + namespace + ":" + recipeId);
    }

    /**
     * Unregisters all campfire recipes this handler has registered.
     */
    public void unregisterAll() {
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();
    }

    private static ItemStack resolveResult(
            ConfigurationSection resultSection,
            String namespace,
            String recipeId
    ) {
        String itemValue = resultSection.getString("item");
        int amount = resultSection.getInt("amount", 1);

        if (itemValue == null) {
            Log.warn(LOG_TAG, "Missing 'result.item' for " + namespace + ":" + recipeId);
            return null;
        }

        ItemStack item = NamespaceUtils.itemByID(namespace, itemValue);
        if (item == null) {
            Log.warn(LOG_TAG, "Could not resolve result item: " + itemValue
                    + " (namespace: " + namespace + ")");
            return null;
        }

        ItemStack result = item.clone();
        result.setAmount(amount);
        return result;
    }
}
