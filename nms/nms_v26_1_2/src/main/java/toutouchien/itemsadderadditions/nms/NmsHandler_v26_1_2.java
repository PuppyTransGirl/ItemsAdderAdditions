package toutouchien.itemsadderadditions.nms;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.*;

@NullMarked
public final class NmsHandler_v26_1_2 implements INmsHandler {
    private final NmsBedHandler_v26_1_2 bed = new NmsBedHandler_v26_1_2();
    private final NmsCampfireRecipeHandler_v26_1_2 campfireRecipes = new NmsCampfireRecipeHandler_v26_1_2();
    private final NmsCreativeMenuHandler_v26_1_2 creativeMenu = new NmsCreativeMenuHandler_v26_1_2();
    private final NmsStonecutterRecipeHandler_v26_1_2 stonecutterRecipes = new NmsStonecutterRecipeHandler_v26_1_2();
    private final NmsToastHandler_v26_1_2 toasts = new NmsToastHandler_v26_1_2();

    @Override
    public NmsBedHandler_v26_1_2 bed() {
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
