package toutouchien.itemsadderadditions.nms.api;

import org.bukkit.inventory.ItemStack;

public interface INmsCampfireRecipeHandler {
    void register(
            String namespace,
            String recipeId,
            ItemStack ingredient,
            ItemStack result,
            int cookTime,
            float exp
    );

    void unregisterAll();
}
