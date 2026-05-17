package toutouchien.itemsadderadditions.nms.api;

import toutouchien.itemsadderadditions.feature.recipe.crafting.CraftingRecipeData;

public interface INmsCraftingRecipeHandler {
    /**
     * Registers a shaped or shapeless crafting recipe with the NMS recipe
     * manager. The recipe key is derived from {@link CraftingRecipeData#key()}.
     *
     * @param data the fully-parsed recipe to register
     */
    void register(CraftingRecipeData data);

    /**
     * Removes every recipe previously registered through this handler.
     */
    void unregisterAll();
}
