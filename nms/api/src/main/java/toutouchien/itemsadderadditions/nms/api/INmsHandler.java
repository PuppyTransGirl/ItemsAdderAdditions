package toutouchien.itemsadderadditions.nms.api;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface INmsHandler {
    INmsBedHandler bed();

    INmsBiomeHandler biome();

    INmsCampfireRecipeHandler campfireRecipes();

    INmsCraftingRecipeHandler craftingRecipes();

    @Nullable INmsCreativeMenuHandler creativeMenu();

    @Nullable INmsPaintingHandler paintings();

    INmsStonecutterRecipeHandler stonecutterRecipes();

    INmsToastHandler toasts();

    /**
     * Calls {@code RecipeManager#finalizeRecipeLoading()} once.
     * Must be invoked by {@code RecipeManager} after <em>all</em> recipe
     * types have been registered or unregistered.
     */
    void finalizeRecipes();
}
