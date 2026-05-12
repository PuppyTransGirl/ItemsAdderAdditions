package toutouchien.itemsadderadditions.nms;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.*;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.recipe.crafting.CraftingRecipeData;
import toutouchien.itemsadderadditions.feature.recipe.crafting.ingredient.ParsedIngredient;
import toutouchien.itemsadderadditions.nms.api.INmsCraftingRecipeHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NullMarked
final class NmsCraftingRecipeHandler_v1_21_4 implements INmsCraftingRecipeHandler {
    private static final String LOG_TAG = "CraftingRecipe";

    private final List<ResourceKey<Recipe<?>>> registeredKeys = new ArrayList<>();

    private static ShapedRecipe buildShaped(CraftingRecipeData data) {
        // pattern() is guaranteed non-null for shaped recipes (see CraftingRecipeData).
        String[] pattern = data.pattern();

        // Build the NMS ShapedRecipePattern.
        // ShapedRecipePattern.of(Map<Character, Ingredient>, String...) is the
        // standard Paper/Mojang factory used internally by DataPack loading.
        Map<Character, Ingredient> nmsIngredients = new HashMap<>();
        for (Map.Entry<Character, ParsedIngredient> entry : data.ingredients().entrySet()) {
            nmsIngredients.put(entry.getKey(), toIngredient(entry.getValue()));
        }

        ShapedRecipePattern nmsPattern = ShapedRecipePattern.of(nmsIngredients, pattern);
        return new ShapedRecipe(
                "", // group (empty = no recipe book group)
                CraftingBookCategory.MISC,
                nmsPattern,
                CraftItemStack.asNMSCopy(data.result())
        );
    }

    private static ShapelessRecipe buildShapeless(CraftingRecipeData data) {
        List<Ingredient> nmsIngredients = new ArrayList<>();

        for (ParsedIngredient parsed : data.ingredients().values()) {
            // Only add the ingredient ONCE to the NMS list.
            // This makes the NMS engine "loose" enough to accept both stacks and
            // spread-out items. The CraftingRecipeListener handles the actual
            // amount enforcement.
            nmsIngredients.add(toIngredient(parsed));
        }

        return new ShapelessRecipe(
                "", // group
                CraftingBookCategory.MISC,
                CraftItemStack.asNMSCopy(data.result()),
                nmsIngredients
        );
    }

    /**
     * Converts a Bukkit {@link RecipeChoice} (stored in
     * {@link ParsedIngredient#choice()}) into an NMS {@link Ingredient}.
     *
     * <ul>
     *   <li>{@link RecipeChoice.ExactChoice} - holds custom IA item stacks;
     *       converted via {@code Ingredient.ofStacks} so the NMS engine
     *       performs an exact-NBT comparison.</li>
     *   <li>{@link RecipeChoice.MaterialChoice} - holds vanilla materials /
     *       tag members; converted via {@code Ingredient.of(ItemType...)} so
     *       any stack of those materials is accepted.</li>
     * </ul>
     */
    private static Ingredient toIngredient(ParsedIngredient parsed) {
        RecipeChoice choice = parsed.choice();

        if (choice instanceof RecipeChoice.ExactChoice exact) {
            // Convert each Bukkit ItemStack to an NMS ItemStack.
            List<net.minecraft.world.item.ItemStack> nmsStacks = exact.getChoices()
                    .stream()
                    .map(CraftItemStack::asNMSCopy)
                    .toList();
            return Ingredient.ofStacks(nmsStacks);
        }

        if (choice instanceof RecipeChoice.MaterialChoice material) {
            // Convert each Bukkit Material to an NMS ItemType (Item).
            // CraftItemType.asNMS(Material) is the canonical bridge in Paper.
            List<net.minecraft.world.item.ItemStack> nmsStacks = material.getChoices()
                    .stream()
                    .map(mat -> new net.minecraft.world.item.ItemStack(
                            org.bukkit.craftbukkit.util.CraftMagicNumbers.getItem(mat)))
                    .toList();
            return Ingredient.ofStacks(nmsStacks);
        }

        // Fallback - should never be reached with the current IngredientResolver.
        throw new IllegalArgumentException(
                "Unsupported RecipeChoice type: " + choice.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private static <T extends RecipeInput> ResourceKey<Recipe<T>> castRecipeKey(ResourceKey<Recipe<?>> key) {
        return (ResourceKey<Recipe<T>>) (ResourceKey<?>) key;
    }

    @Override
    public void register(CraftingRecipeData data) {
        // Derive an NMS Identifier from the Bukkit NamespacedKey already in CraftingRecipeData.
        ResourceLocation identifier = ResourceLocation.fromNamespaceAndPath(
                "iaadditions",
                data.key().getKey() // key().getKey() is already "namespace_recipeId"
        );
        ResourceKey<Recipe<?>> resourceKey = ResourceKey.create(Registries.RECIPE, identifier);

        Recipe<?> recipe = data.shaped()
                ? buildShaped(data)
                : buildShapeless(data);

        MinecraftServer.getServer()
                .getRecipeManager()
                .recipes
                .addRecipe(new RecipeHolder<>(resourceKey, recipe));

        registeredKeys.add(resourceKey);
        Log.debug(LOG_TAG, "Registered: " + data.key());
    }

    @Override
    public void unregisterAll() {
        RecipeManager recipeManager = MinecraftServer.getServer().getRecipeManager();
        for (ResourceKey<Recipe<?>> key : registeredKeys)
            recipeManager.recipes.removeRecipe(castRecipeKey(key));

        registeredKeys.clear();
        // Finalization is intentionally omitted here - RecipeManager calls
        // INmsHandler#finalizeRecipes() once after all handlers are done.
    }
}
