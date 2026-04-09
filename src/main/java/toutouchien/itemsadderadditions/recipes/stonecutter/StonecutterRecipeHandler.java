package toutouchien.itemsadderadditions.recipes.stonecutter;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the {@code recipes.stonecutter} YAML section and registers
 * {@link StonecutterRecipe} entries into the server.
 */
public class StonecutterRecipeHandler {
    private static final String LOG_TAG = "StonecutterRecipe";
    public static final String KEY_PREFIX = "iaa_stonecutter_";

    private final List<ResourceKey<Recipe<?>>> registeredKeys = new ArrayList<>();

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
            if (ingredient == null) continue;

            ConfigurationSection resultSection = entry.getConfigurationSection("result");
            if (resultSection == null) {
                Log.warn(LOG_TAG, "Missing 'result' for " + namespace + ":" + recipeId);
                continue;
            }

            ItemStack result = resolveResult(resultSection, namespace, recipeId);
            if (result == null) continue;

            register(namespace, recipeId, ingredient, result);
        }
    }

    private void register(
            String namespace,
            String recipeID,
            ItemStack ingredient,
            ItemStack result
    ) {
        Identifier identifier = Identifier.fromNamespaceAndPath(
                "iaadditions",
                "iaa_stonecutter_" + namespace + "_" + recipeID
        );
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, identifier);

        StonecutterRecipe recipe = new StonecutterRecipe(
                "",
                Ingredient.of(CraftItemStack.asNMSCopy(ingredient).getItem()),
                CraftItemStack.asNMSCopy(result)
        );

        RecipeManager recipeManager = MinecraftServer.getServer().getRecipeManager();
        recipeManager.addRecipe(new RecipeHolder<>(
                key, recipe
        ));

        registeredKeys.add(key);

        Log.info(LOG_TAG, "Registered stonecutter recipe: " + namespace + ":" + recipeID);
    }

    /**
     * Unregisters all stonecutter recipes this handler has registered.
     */
    public void unregisterAll() {
        for (ResourceKey<Recipe<?>> key : registeredKeys)
            MinecraftServer.getServer().getRecipeManager().removeRecipe(key);

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
            Log.warn(LOG_TAG, "Could not resolve result item: '" + itemValue
                    + "' (namespace: " + namespace + ")");
            return null;
        }

        item.setAmount(amount);
        return item;
    }
}
