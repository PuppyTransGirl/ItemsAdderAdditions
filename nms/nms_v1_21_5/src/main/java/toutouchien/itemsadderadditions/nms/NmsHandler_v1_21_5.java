package toutouchien.itemsadderadditions.nms;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.*;

@NullMarked
public final class NmsHandler_v1_21_5 implements INmsHandler {
    private final NmsBedHandler_v1_21_5 bed = new NmsBedHandler_v1_21_5();
    private final NmsCampfireRecipeHandler_v1_21_5 campfireRecipes = new NmsCampfireRecipeHandler_v1_21_5();
    private final NmsCreativeMenuHandler_v1_21_5 creativeMenu = new NmsCreativeMenuHandler_v1_21_5();
    private final NmsStonecutterRecipeHandler_v1_21_5 stonecutterRecipes = new NmsStonecutterRecipeHandler_v1_21_5();
    private final NmsToastHandler_v1_21_5 toasts = new NmsToastHandler_v1_21_5();

    @Override
    public NmsBedHandler_v1_21_5 bed() {
        return bed;
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
