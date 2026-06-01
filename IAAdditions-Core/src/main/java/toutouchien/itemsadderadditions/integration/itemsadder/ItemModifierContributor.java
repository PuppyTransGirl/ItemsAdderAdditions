package toutouchien.itemsadderadditions.integration.itemsadder;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@FunctionalInterface
@NullMarked
public interface ItemModifierContributor {
    ItemStack apply(String namespacedId, ItemStack itemStack);
}
