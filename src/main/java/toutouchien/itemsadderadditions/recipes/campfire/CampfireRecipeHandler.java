package toutouchien.itemsadderadditions.recipes.campfire;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

public class CampfireRecipeHandler {
    private static final String LOG_TAG = "CampfireRecipe";

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
            Log.warn(LOG_TAG, "Could not resolve result item: '" + itemValue + "' (namespace: " + namespace + ")");
            return null;
        }

        item.setAmount(amount);
        return item;
    }

    public void load(String namespace, ConfigurationSection section) {
        if (section == null) return;

        for (String recipeId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(recipeId);
            if (entry == null) continue;
            if (!entry.getBoolean("enabled", true)) continue;

            ConfigurationSection ingredientSection = entry.getConfigurationSection("ingredient");
            if (ingredientSection == null) {
                Log.warn(LOG_TAG, "Missing 'ingredient' for " + namespace + ":" + recipeId);
                continue;
            }

            ItemStack ingredient = NamespaceUtils.itemByID(namespace, ingredientSection.getString("item"));
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

            NmsManager.instance()
                    .handler()
                    .campfireRecipes()
                    .register(namespace, recipeId, ingredient, result, cookTime, exp);
        }
    }

    public void unregisterAll() {
        NmsManager.instance().handler().campfireRecipes().unregisterAll();
    }
}
