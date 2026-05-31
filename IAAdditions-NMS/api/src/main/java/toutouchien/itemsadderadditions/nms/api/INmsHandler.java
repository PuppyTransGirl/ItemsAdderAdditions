package toutouchien.itemsadderadditions.nms.api;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.nms.api.component.INmsItemComponentHandler;

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

    INmsTextDisplayHandler textDisplays();

    INmsAdvancementHandler advancements();

    /**
     * Returns the NMS-backed handler for generic item components.
     * Implementations for versions that do not support the codec pipeline
     * return a handler where {@link INmsItemComponentHandler#isSupported()} is false.
     */
    INmsItemComponentHandler itemComponents();

    /**
     * Calls {@code RecipeManager#finalizeRecipeLoading()} once.
     * Must be invoked by {@code RecipeManager} after <em>all</em> recipe
     * types have been registered or unregistered.
     */
    void finalizeRecipes();
}
