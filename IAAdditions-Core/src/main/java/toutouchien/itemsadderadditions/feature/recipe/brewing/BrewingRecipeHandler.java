package toutouchien.itemsadderadditions.feature.recipe.brewing;

import io.papermc.paper.potion.PotionMix;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentsManager;
import toutouchien.itemsadderadditions.feature.recipe.AbstractRecipeHandler;
import toutouchien.itemsadderadditions.feature.recipe.RecipeActions;
import toutouchien.itemsadderadditions.feature.recipe.RecipeActionsParser;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public final class BrewingRecipeHandler extends AbstractRecipeHandler {
    public interface BrewingMixRegistry {
        void add(PotionMix mix);

        void remove(NamespacedKey key);
    }

    private static final BrewingMixRegistry BUKKIT_MIX_REGISTRY = new BrewingMixRegistry() {
        @Override
        public void add(PotionMix mix) {
            Bukkit.getPotionBrewer().addPotionMix(mix);
        }

        @Override
        public void remove(NamespacedKey key) {
            Bukkit.getPotionBrewer().removePotionMix(key);
        }
    };

    private final List<BrewingRecipeData> recipes = new ArrayList<>();
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();
    private final BrewingMixRegistry mixRegistry;

    public BrewingRecipeHandler() {
        this(null, BUKKIT_MIX_REGISTRY);
    }

    public BrewingRecipeHandler(BrewingMixRegistry mixRegistry) {
        this(null, mixRegistry);
    }

    public BrewingRecipeHandler(@Nullable ComponentsManager componentsManager) {
        this(componentsManager, BUKKIT_MIX_REGISTRY);
    }

    public BrewingRecipeHandler(@Nullable ComponentsManager componentsManager, BrewingMixRegistry mixRegistry) {
        super("BrewingRecipe", componentsManager);
        this.mixRegistry = mixRegistry;
    }

    public List<BrewingRecipeData> recipes() {
        return recipes;
    }

    @Nullable
    public BrewingRecipeMatch match(org.bukkit.inventory.BrewerInventory inventory) {
        ItemStack ingredient = inventory.getIngredient();
        if (isAir(ingredient)) return null;

        for (BrewingRecipeData recipe : recipes) {
            if (!recipe.ingredient().isSimilar(ingredient)) continue;
            if (ingredient.getAmount() < recipe.ingredientConsume()) continue;

            boolean[] slots = new boolean[3];
            int matched = 0;
            for (int slot = 0; slot < 3; slot++) {
                ItemStack bottle = inventory.getItem(slot);
                if (!isAir(bottle) && recipe.base().isSimilar(bottle)) {
                    slots[slot] = true;
                    matched++;
                }
            }
            if (matched > 0) return new BrewingRecipeMatch(recipe, slots, matched);
        }

        return null;
    }

    @Override
    protected void loadEntry(String namespace, String recipeId, ConfigurationSection entry) {
        ConfigurationSection baseSection = entry.getConfigurationSection("base");
        String baseRole = "base";
        if (baseSection == null) {
            baseSection = entry.getConfigurationSection("input");
            baseRole = "input";
        }
        if (baseSection == null) {
            Log.warn(logTag, "Missing 'base'/'input' for {}:{}", namespace, recipeId);
            return;
        }

        ItemStack base = resolveItem(namespace, recipeId, baseRole, baseSection.getString("item"));
        if (base == null) return;

        ConfigurationSection ingredientSection = entry.getConfigurationSection("ingredient");
        if (ingredientSection == null) {
            Log.warn(logTag, "Missing 'ingredient' for {}:{}", namespace, recipeId);
            return;
        }
        ItemStack ingredient = resolveIngredient(namespace, recipeId, ingredientSection);
        if (ingredient == null) return;

        int consume = ingredientSection.getInt("consume", 1);
        if (consume <= 0) {
            Log.warn(logTag, "Invalid 'ingredient.consume' {} for {}:{}; must be positive.",
                    consume, namespace, recipeId);
            return;
        }

        ConfigurationSection resultSection = entry.getConfigurationSection("result");
        if (resultSection == null) {
            Log.warn(logTag, "Missing 'result' for {}:{}", namespace, recipeId);
            return;
        }
        ItemStack result = resolveResult(namespace, recipeId, resultSection);
        if (result == null) return;
        if (result.getAmount() != 1) {
            Log.warn(logTag, "Result amount for {}:{} is {}; brewing slots hold 1, clamping to 1.",
                    namespace, recipeId, result.getAmount());
            result.setAmount(1);
        }

        int brewTime = entry.getInt("brew_time", 400);
        if (brewTime <= 0) {
            Log.warn(logTag, "Invalid 'brew_time' {} for {}:{}; must be positive.",
                    brewTime, namespace, recipeId);
            return;
        }

        int fuelCost = entry.getInt("fuel_cost", 1);
        if (fuelCost <= 0) {
            Log.warn(logTag, "Invalid 'fuel_cost' {} for {}:{}; must be positive.",
                    fuelCost, namespace, recipeId);
            return;
        }

        RecipeActions actions = RecipeActionsParser.parse(entry.getConfigurationSection("on_complete"));
        BrewingRecipeData data = new BrewingRecipeData(
                namespace + ":" + recipeId,
                base.clone(),
                ingredient.clone(),
                consume,
                result.clone(),
                brewTime,
                fuelCost,
                actions
        );

        registerMix(namespace, recipeId, data.base(), data.ingredient(), data.result());
        recipes.add(data);
        incrementCount();
        Log.debug(logTag, "Registered: {}:{}", namespace, recipeId);
    }

    @Override
    protected void registerRecipe(
            String namespace,
            String recipeId,
            ConfigurationSection entry,
            ItemStack ingredient,
            ItemStack result
    ) {
        throw new UnsupportedOperationException("Brewing recipes use a custom parser");
    }

    @Override
    public void unregisterAll() {
        for (NamespacedKey key : registeredKeys) {
            mixRegistry.remove(key);
        }
        registeredKeys.clear();
        recipes.clear();
        resetCount();
    }

    private void registerMix(
            String namespace,
            String recipeId,
            ItemStack base,
            ItemStack ingredient,
            ItemStack result
    ) {
        NamespacedKey key = new NamespacedKey(
                "iaadditions",
                ("iaa_brewing_" + namespace + "_" + recipeId).toLowerCase()
        );

        RecipeChoice baseChoice = PotionMix.createPredicateChoice(base::isSimilar);
        RecipeChoice ingredientChoice = PotionMix.createPredicateChoice(ingredient::isSimilar);
        PotionMix mix = new PotionMix(key, result.clone(), baseChoice, ingredientChoice);

        mixRegistry.add(mix);
        registeredKeys.add(key);
    }

    private static boolean isAir(@Nullable ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
