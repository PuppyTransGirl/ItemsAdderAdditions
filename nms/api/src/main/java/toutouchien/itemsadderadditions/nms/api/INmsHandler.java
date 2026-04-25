package toutouchien.itemsadderadditions.nms.api;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface INmsHandler {
    INmsBedHandler bed();

    INmsBiomeHandler biome();

    INmsCampfireRecipeHandler campfireRecipes();

    @Nullable
    INmsCreativeMenuHandler creativeMenu();

    INmsStonecutterRecipeHandler stonecutterRecipes();

    INmsToastHandler toasts();
}
