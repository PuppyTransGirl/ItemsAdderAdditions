package toutouchien.itemsadderadditions.nms;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.*;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import toutouchien.itemsadderadditions.nms.api.INmsCampfireRecipeHandler;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.List;

final class NmsCampfireRecipeHandler_v1_21_3 implements INmsCampfireRecipeHandler {
    private static final String LOG_TAG = "CampfireRecipe";

    private final List<ResourceKey<Recipe<?>>> registeredKeys = new ArrayList<>();

    @Override
    public void register(
            String namespace,
            String recipeId,
            ItemStack ingredient,
            ItemStack result,
            int cookTime,
            float exp
    ) {
        ResourceLocation identifier = ResourceLocation.fromNamespaceAndPath(
                "iaadditions",
                "iaa_campfire_" + namespace + "_" + recipeId
        );
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, identifier);

        CampfireCookingRecipe recipe = new CampfireCookingRecipe(
                "",
                CookingBookCategory.MISC,
                Ingredient.ofStacks(List.of(CraftItemStack.asNMSCopy(ingredient))),
                CraftItemStack.asNMSCopy(result),
                exp,
                cookTime
        );

        MinecraftServer.getServer()
                .getRecipeManager()
                .addRecipe(new RecipeHolder<>(key, recipe));

        registeredKeys.add(key);
        Log.info(LOG_TAG, "Registered campfire recipe: " + namespace + ":" + recipeId);
    }

    @Override
    public void unregisterAll() {
        RecipeManager recipeManager = MinecraftServer.getServer().getRecipeManager();
        for (ResourceKey<Recipe<?>> key : registeredKeys)
            recipeManager.removeRecipe(key);

        registeredKeys.clear();
    }
}
