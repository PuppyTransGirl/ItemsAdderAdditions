package toutouchien.itemsadderadditions.nms;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.*;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import toutouchien.itemsadderadditions.nms.api.INmsCampfireRecipeHandler;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.List;

final class NmsCampfireRecipeHandler_v1_20_6 implements INmsCampfireRecipeHandler {
    private static final String LOG_TAG = "CampfireRecipe";

    private final List<ResourceLocation> registeredKeys = new ArrayList<>();

    @Override
    public void register(
            String namespace,
            String recipeId,
            ItemStack ingredient,
            ItemStack result,
            int cookTime,
            float exp
    ) {
        ResourceLocation identifier = ResourceLocation.of(
                "iaadditions:iaa_campfire_" + namespace + "_" + recipeId,
                ':'
        );

        CampfireCookingRecipe recipe = new CampfireCookingRecipe(
                "",
                CookingBookCategory.MISC,
                Ingredient.of(CraftItemStack.asNMSCopy(ingredient)),
                CraftItemStack.asNMSCopy(result),
                exp,
                cookTime
        );

        // Use the internal recipes field directly to avoid per-recipe finalization.
        MinecraftServer.getServer()
                .getRecipeManager()
                .addRecipe(new RecipeHolder<>(identifier, recipe));

        registeredKeys.add(identifier);
        Log.debug(LOG_TAG, "Registered: " + namespace + ":" + recipeId);
    }

    @Override
    public void unregisterAll() {
        RecipeManager recipeManager = MinecraftServer.getServer().getRecipeManager();
        for (ResourceLocation key : registeredKeys)
            recipeManager.removeRecipe(key);

        registeredKeys.clear();
        // Finalization is intentionally omitted here - RecipeManager calls
        // INmsHandler#finalizeRecipes() once after all handlers are done.
    }
}
