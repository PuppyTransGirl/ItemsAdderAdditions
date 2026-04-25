package toutouchien.itemsadderadditions.nms;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.*;

@NullMarked
public final class NmsHandler_v1_21_11 implements INmsHandler {
    private final NmsBedHandler_v1_21_11 bed = new NmsBedHandler_v1_21_11();
    private final NmsBiomeHandler_v1_21_11 biome = new NmsBiomeHandler_v1_21_11();
    private final NmsCampfireRecipeHandler_v1_21_11 campfireRecipes = new NmsCampfireRecipeHandler_v1_21_11();
    private final NmsCreativeMenuHandler_v1_21_11 creativeMenu = new NmsCreativeMenuHandler_v1_21_11();
    private final NmsStonecutterRecipeHandler_v1_21_11 stonecutterRecipes = new NmsStonecutterRecipeHandler_v1_21_11();
    private final NmsToastHandler_v1_21_11 toasts = new NmsToastHandler_v1_21_11();

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
}
