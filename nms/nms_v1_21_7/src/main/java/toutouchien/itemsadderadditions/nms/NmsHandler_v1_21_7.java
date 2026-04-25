package toutouchien.itemsadderadditions.nms;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.*;

@NullMarked
public final class NmsHandler_v1_21_7 implements INmsHandler {
    private final NmsBedHandler_v1_21_7 bed = new NmsBedHandler_v1_21_7();
    private final NmsBiomeHandler_v1_21_7 biome = new NmsBiomeHandler_v1_21_7();
    private final NmsCampfireRecipeHandler_v1_21_7 campfireRecipes = new NmsCampfireRecipeHandler_v1_21_7();
    private final NmsCreativeMenuHandler_v1_21_7 creativeMenu = new NmsCreativeMenuHandler_v1_21_7();
    private final NmsStonecutterRecipeHandler_v1_21_7 stonecutterRecipes = new NmsStonecutterRecipeHandler_v1_21_7();
    private final NmsToastHandler_v1_21_7 toasts = new NmsToastHandler_v1_21_7();

    @Override
    public NmsBedHandler_v1_21_7 bed() {
        return bed;
    }

    @Override
    public NmsBiomeHandler_v1_21_7 biome() {
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
