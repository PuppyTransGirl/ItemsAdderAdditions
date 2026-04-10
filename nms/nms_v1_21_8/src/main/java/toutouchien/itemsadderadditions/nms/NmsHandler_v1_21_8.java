package toutouchien.itemsadderadditions.nms;

import toutouchien.itemsadderadditions.nms.api.*;

public final class NmsHandler_v1_21_8 implements INmsHandler {
    private final NmsCampfireRecipeHandler_v1_21_8 campfireRecipes = new NmsCampfireRecipeHandler_v1_21_8();
    private final NmsCreativeMenuHandler_v1_21_8 creativeMenu = new NmsCreativeMenuHandler_v1_21_8();
    private final NmsStonecutterRecipeHandler_v1_21_8 stonecutterRecipes = new NmsStonecutterRecipeHandler_v1_21_8();
    private final NmsToastHandler_v1_21_8 toasts = new NmsToastHandler_v1_21_8();

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
