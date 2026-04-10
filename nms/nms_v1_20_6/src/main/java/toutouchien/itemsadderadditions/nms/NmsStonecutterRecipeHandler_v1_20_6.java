package toutouchien.itemsadderadditions.nms;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import toutouchien.itemsadderadditions.nms.api.INmsStonecutterRecipeHandler;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.List;

final class NmsStonecutterRecipeHandler_v1_20_6 implements INmsStonecutterRecipeHandler {
    private static final String LOG_TAG = "StonecutterRecipe";

    private final List<ResourceLocation> registeredKeys = new ArrayList<>();

    @Override
    public void register(
            String namespace,
            String recipeId,
            ItemStack ingredient,
            ItemStack result
    ) {
        ResourceLocation identifier = ResourceLocation.of(
                "iaadditionsiaa_stonecutter_" + namespace + "_" + recipeId,
                ':'
        );

        StonecutterRecipe recipe = new StonecutterRecipe(
                "",
                Ingredient.of(CraftItemStack.asNMSCopy(ingredient)),
                CraftItemStack.asNMSCopy(result)
        );

        MinecraftServer.getServer()
                .getRecipeManager()
                .addRecipe(new RecipeHolder<>(identifier, recipe));

        registeredKeys.add(identifier);
        Log.info(LOG_TAG, "Registered stonecutter recipe: " + namespace + ":" + recipeId);
    }

    @Override
    public void unregisterAll() {
        RecipeManager recipeManager = MinecraftServer.getServer().getRecipeManager();
        for (ResourceLocation key : registeredKeys)
            recipeManager.removeRecipe(key);

        registeredKeys.clear();
    }
}
