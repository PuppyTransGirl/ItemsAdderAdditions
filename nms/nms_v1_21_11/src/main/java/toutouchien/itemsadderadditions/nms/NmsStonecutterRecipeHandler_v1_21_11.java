package toutouchien.itemsadderadditions.nms;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.*;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.nms.api.INmsStonecutterRecipeHandler;

import java.util.ArrayList;
import java.util.List;

final class NmsStonecutterRecipeHandler_v1_21_11 implements INmsStonecutterRecipeHandler {
    private static final String LOG_TAG = "StonecutterRecipe";

    private final List<ResourceKey<Recipe<?>>> registeredKeys = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private static <T extends RecipeInput> ResourceKey<Recipe<T>> castRecipeKey(
            ResourceKey<Recipe<?>> key) {
        return (ResourceKey<Recipe<T>>) (ResourceKey<?>) key;
    }

    @Override
    public void register(
            String namespace,
            String recipeId,
            ItemStack ingredient,
            ItemStack result
    ) {
        Identifier identifier = Identifier.fromNamespaceAndPath(
                "iaadditions",
                "iaa_stonecutter_" + namespace + "_" + recipeId
        );
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, identifier);

        StonecutterRecipe recipe = new StonecutterRecipe(
                "",
                Ingredient.ofStacks(List.of(CraftItemStack.asNMSCopy(ingredient))),
                CraftItemStack.asNMSCopy(result)
        );

        // Use the internal recipes field directly to avoid per-recipe finalization.
        MinecraftServer.getServer()
                .getRecipeManager()
                .recipes
                .addRecipe(new RecipeHolder<>(key, recipe));

        registeredKeys.add(key);
        Log.debug(LOG_TAG, "Registered: " + namespace + ":" + recipeId);
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
