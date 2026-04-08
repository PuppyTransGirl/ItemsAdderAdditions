package toutouchien.itemsadderadditions.recipes.stonecutter;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.StonecuttingRecipe;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.recipes.RecipeItemResolver;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the {@code recipes.stonecutter} YAML section and registers
 * {@link StonecuttingRecipe} entries into the server.
 */
public class StonecutterRecipeHandler {
    private static final String LOG_TAG = "StonecutterRecipe";
    public static final String KEY_PREFIX = "iaa_stonecutter_";

    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    /**
     * Parses the {@code stonecutter} sub-section of a namespace YAML file.
     *
     * @param namespace The IA namespace
     * @param section   The {@code recipes.stonecutter} ConfigurationSection
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
            RecipeChoice ingredient = RecipeItemResolver.resolve(namespace, ingredientValue, LOG_TAG);
            if (ingredient == null) continue;

            ConfigurationSection resultSection = entry.getConfigurationSection("result");
            if (resultSection == null) {
                Log.warn(LOG_TAG, "Missing 'result' for " + namespace + ":" + recipeId);
                continue;
            }
            ItemStack result = resolveResult(resultSection, namespace, recipeId);
            if (result == null) continue;

            String permission = entry.getString("permission", null);
            register(namespace, recipeId, ingredient, result, permission);
        }
    }

    private void register(
            String namespace,
            String recipeId,
            RecipeChoice ingredient,
            ItemStack result,
            String permission
    ) {
        NamespacedKey key = new NamespacedKey(
                ItemsAdderAdditions.instance(),
                KEY_PREFIX + namespace + "_" + recipeId
        );

        Bukkit.removeRecipe(key);

        StonecuttingRecipe recipe = new StonecuttingRecipe(key, result, ingredient);

        Bukkit.addRecipe(recipe);
        registeredKeys.add(key);

        Log.info(LOG_TAG, "Registered stonecutter recipe: " + namespace + ":" + recipeId);
    }

    /**
     * Unregisters all stonecutter recipes this handler has registered.
     */
    public void unregisterAll() {
        for (NamespacedKey key : registeredKeys)
            Bukkit.removeRecipe(key);

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

        // Fallback to minecraft: namespace for bare vanilla names
        if (item == null && !itemValue.contains(":")) {
            item = NamespaceUtils.itemByID("minecraft", itemValue);
        }

        if (item == null) {
            Log.warn(LOG_TAG, "Could not resolve result item: '" + itemValue
                    + "' (namespace: " + namespace + ")");
            return null;
        }

        ItemStack result = item.clone();
        result.setAmount(amount);
        return result;
    }
}
