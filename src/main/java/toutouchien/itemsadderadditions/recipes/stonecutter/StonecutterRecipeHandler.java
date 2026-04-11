package toutouchien.itemsadderadditions.recipes.stonecutter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

public class StonecutterRecipeHandler {
    private static final String LOG_TAG = "StonecutterRecipe";

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
            Log.warn(LOG_TAG, "Could not resolve result item: '" + itemValue
                    + "' (namespace: " + namespace + ")");
            return null;
        }

        item = item.clone();
        item.setAmount(amount);
        return item;
    }

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
            ItemStack ingredient = NamespaceUtils.itemByID(namespace, ingredientValue);
            if (ingredient == null) {
                Log.warn(LOG_TAG, "Could not resolve ingredient item: '" + ingredientValue
                        + "' (namespace: " + namespace + ")");
                continue;
            }

            ingredient = ingredient.clone();

            ConfigurationSection resultSection = entry.getConfigurationSection("result");
            if (resultSection == null) {
                Log.warn(LOG_TAG, "Missing 'result' for " + namespace + ":" + recipeId);
                continue;
            }

            ItemStack result = resolveResult(resultSection, namespace, recipeId);
            if (result == null) continue;

            NmsManager.instance()
                    .handler()
                    .stonecutterRecipes()
                    .register(namespace, recipeId, ingredient, result);

            StonecutterPatchBridge.register(ingredient, result);
        }
    }

    public void unregisterAll() {
        NmsManager.instance().handler().stonecutterRecipes().unregisterAll();
        StonecutterPatchBridge.clear();
    }
}
