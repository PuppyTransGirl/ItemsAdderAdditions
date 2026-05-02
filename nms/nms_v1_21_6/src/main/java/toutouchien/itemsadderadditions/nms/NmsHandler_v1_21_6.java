package toutouchien.itemsadderadditions.nms;

import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.*;

@NullMarked
public final class NmsHandler_v1_21_6 implements INmsHandler {
    private final NmsBedHandler_v1_21_6 bed = new NmsBedHandler_v1_21_6();
    private final NmsBiomeHandler_v1_21_6 biome = new NmsBiomeHandler_v1_21_6();
    private final NmsCampfireRecipeHandler_v1_21_6 campfireRecipes = new NmsCampfireRecipeHandler_v1_21_6();
    private final NmsCraftingRecipeHandler_v1_21_6 craftingRecipes = new NmsCraftingRecipeHandler_v1_21_6();
    private final NmsCreativeMenuHandler_v1_21_6 creativeMenu = new NmsCreativeMenuHandler_v1_21_6();
    private final NmsStonecutterRecipeHandler_v1_21_6 stonecutterRecipes = new NmsStonecutterRecipeHandler_v1_21_6();
    private final NmsToastHandler_v1_21_6 toasts = new NmsToastHandler_v1_21_6();

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
