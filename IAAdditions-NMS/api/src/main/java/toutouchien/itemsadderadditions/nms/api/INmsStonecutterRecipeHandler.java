package toutouchien.itemsadderadditions.nms.api;

import org.bukkit.inventory.ItemStack;

public interface INmsStonecutterRecipeHandler {
    void register(
            String namespace,
            String recipeID,
            ItemStack ingredient,
            ItemStack result
    );

    void unregisterAll();
}
