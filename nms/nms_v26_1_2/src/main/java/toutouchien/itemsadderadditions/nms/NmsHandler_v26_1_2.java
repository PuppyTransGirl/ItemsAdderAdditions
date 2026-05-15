package toutouchien.itemsadderadditions.nms;

import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.*;

@NullMarked
public final class NmsHandler_v26_1_2 implements INmsHandler {
    private final NmsBedHandler_v26_1_2 bed = new NmsBedHandler_v26_1_2();
    private final NmsBiomeHandler_v26_1_2 biome = new NmsBiomeHandler_v26_1_2();
    private final NmsCampfireRecipeHandler_v26_1_2 campfireRecipes = new NmsCampfireRecipeHandler_v26_1_2();
    private final NmsCraftingRecipeHandler_v26_1_2 craftingRecipes = new NmsCraftingRecipeHandler_v26_1_2();
    private final NmsCreativeMenuHandler_v26_1_2 creativeMenu = new NmsCreativeMenuHandler_v26_1_2();
    private final NmsPaintingHandler_v26_1_2 paintings = new NmsPaintingHandler_v26_1_2();
    private final NmsStonecutterRecipeHandler_v26_1_2 stonecutterRecipes = new NmsStonecutterRecipeHandler_v26_1_2();
    private final NmsToastHandler_v26_1_2 toasts = new NmsToastHandler_v26_1_2();
    private final NmsTextDisplayHandler_v26_1_2 textDisplays = new NmsTextDisplayHandler_v26_1_2();

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

    @Override
    public INmsCreativeMenuHandler creativeMenu() {
        return creativeMenu;
    }


    @Override
    public INmsPaintingHandler paintings() {
        return paintings;
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
    public INmsTextDisplayHandler textDisplays() {
        return textDisplays;
    }

    @Override
    public void finalizeRecipes() {
        MinecraftServer.getServer().getRecipeManager().finalizeRecipeLoading();
    }
}
