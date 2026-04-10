package toutouchien.itemsadderadditions.nms.api;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface INmsHandler {
    INmsCampfireRecipeHandler campfireRecipes();

    @Nullable
    INmsCreativeMenuHandler creativeMenu();

    INmsStonecutterRecipeHandler stonecutterRecipes();

    INmsToastHandler toasts();
}
