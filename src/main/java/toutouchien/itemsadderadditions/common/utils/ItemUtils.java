package toutouchien.itemsadderadditions.common.utils;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Utility methods for inventory and item manipulation.
 */
@NullMarked
public final class ItemUtils {
    private ItemUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Removes up to {@code amountToRemove} items matching {@code template} from
     * the entity's inventory.
     *
     * <p>Matching is done with {@link ItemStack#isSimilar(ItemStack)} - type, meta,
     * and custom data are compared but stack size is ignored, so a template stack of
     * size 1 will correctly match and remove items from stacks of any size.
     *
     * @param human          the inventory owner
     * @param template       the item to match against (stack size is ignored)
     * @param amountToRemove how many items to remove in total
     */
    public static void removeItemsFromInventory(HumanEntity human, ItemStack template, int amountToRemove) {
        int remaining = amountToRemove;
        @Nullable ItemStack[] contents = human.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !stack.isSimilar(template))
                continue;

            int toRemove = Math.min(remaining, stack.getAmount());
            remaining -= toRemove;

            if (stack.getAmount() <= toRemove)
                human.getInventory().setItem(i, null);
            else
                stack.setAmount(stack.getAmount() - toRemove);
        }
    }
}

