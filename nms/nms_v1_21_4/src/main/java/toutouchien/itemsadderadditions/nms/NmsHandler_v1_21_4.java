package toutouchien.itemsadderadditions.nms;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.nms.api.*;

@NullMarked
public final class NmsHandler_v1_21_4 implements INmsHandler {
    private final NmsBedHandler_v1_21_4 bed = new NmsBedHandler_v1_21_4();
    private final NmsCampfireRecipeHandler_v1_21_4 campfireRecipes = new NmsCampfireRecipeHandler_v1_21_4();
    private final NmsStonecutterRecipeHandler_v1_21_4 stonecutterRecipes = new NmsStonecutterRecipeHandler_v1_21_4();
    private final NmsToastHandler_v1_21_4 toasts = new NmsToastHandler_v1_21_4();

    @Override
    public NmsBedHandler_v1_21_4 bed() {
        return bed;
    }

    @Override
    public INmsCampfireRecipeHandler campfireRecipes() {
        return campfireRecipes;
    }

    // TODO: Add compatibility of creative menu with 1.21.4 and lower with the component ENTITY_DATA
    @Override
    @Nullable
    public INmsCreativeMenuHandler creativeMenu() {
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
}
