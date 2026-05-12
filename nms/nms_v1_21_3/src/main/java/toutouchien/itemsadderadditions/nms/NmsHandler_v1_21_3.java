package toutouchien.itemsadderadditions.nms;

import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.nms.api.*;

@NullMarked
public final class NmsHandler_v1_21_3 implements INmsHandler {
    private final NmsBedHandler_v1_21_3 bed = new NmsBedHandler_v1_21_3();
    private final NmsBiomeHandler_v1_21_3 biome = new NmsBiomeHandler_v1_21_3();
    private final NmsCampfireRecipeHandler_v1_21_3 campfireRecipes = new NmsCampfireRecipeHandler_v1_21_3();
    private final NmsCraftingRecipeHandler_v1_21_3 craftingRecipes = new NmsCraftingRecipeHandler_v1_21_3();
    private final NmsStonecutterRecipeHandler_v1_21_3 stonecutterRecipes = new NmsStonecutterRecipeHandler_v1_21_3();
    private final NmsToastHandler_v1_21_3 toasts = new NmsToastHandler_v1_21_3();

    @Override
    public INmsBedHandler bed() {
        return bed;
    }

    @Override
    public INmsBiomeHandler biome() {
        return biome;
    }

    @Override
    public INmsCampfireRecipeHandler campfireRecipes() {
        return campfireRecipes;
    }

    @Override
    public INmsCraftingRecipeHandler craftingRecipes() {
        return craftingRecipes;
    }

    // TODO: Add compatibility of creative menu with 1.21.4 and lower with the component ENTITY_DATA
    @Override
    @Nullable
    public INmsCreativeMenuHandler creativeMenu() {
        return null;
    }


    @Override
    @Nullable
    public INmsPaintingHandler paintings() {
        return null;
    }

    @Override
    public INmsStonecutterRecipeHandler stonecutterRecipes() {
        return stonecutterRecipes;
    }

    @Override
    public INmsToastHandler toasts() {
        return toasts;
    }

    @Override
    public void finalizeRecipes() {
        MinecraftServer.getServer().getRecipeManager().finalizeRecipeLoading();
    }
}
