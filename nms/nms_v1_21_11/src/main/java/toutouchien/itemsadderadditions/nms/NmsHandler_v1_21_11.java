package toutouchien.itemsadderadditions.nms;

import toutouchien.itemsadderadditions.nms.api.*;

public final class NmsHandler_v1_21_11 implements INmsHandler {
    private final NmsCampfireRecipeHandler_v1_21_11 campfireRecipes = new NmsCampfireRecipeHandler_v1_21_11();
    private final NmsCreativeMenuHandler_v1_21_11 creativeMenu = new NmsCreativeMenuHandler_v1_21_11();
    private final NmsStonecutterRecipeHandler_v1_21_11 stonecutterRecipes = new NmsStonecutterRecipeHandler_v1_21_11();
    private final NmsToastHandler_v1_21_11 toasts = new NmsToastHandler_v1_21_11();

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
