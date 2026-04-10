package toutouchien.itemsadderadditions.nms;

import toutouchien.itemsadderadditions.nms.api.*;

public final class NmsHandler_v1_21_10 implements INmsHandler {
    private final NmsCampfireRecipeHandler_v1_21_10 campfireRecipes = new NmsCampfireRecipeHandler_v1_21_10();
    private final NmsCreativeMenuHandler_v1_21_10 creativeMenu = new NmsCreativeMenuHandler_v1_21_10();
    private final NmsStonecutterRecipeHandler_v1_21_10 stonecutterRecipes = new NmsStonecutterRecipeHandler_v1_21_10();
    private final NmsToastHandler_v1_21_10 toasts = new NmsToastHandler_v1_21_10();

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
