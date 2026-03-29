package toutouchien.itemsadderadditions.utils;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ItemUtils {
    private ItemUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void removeItemsFromInventory(HumanEntity human, ItemStack item, int amountToRemove) {
        int removed = 0;
        @Nullable ItemStack[] contents = human.getInventory().getContents();

        for (int i = 0; i < contents.length && removed < amountToRemove; i++) {
            ItemStack stack = contents[i];
            if (stack == null)
                continue;

            if (stack.equals(item)) {
                int stackAmount = stack.getAmount();
                int toRemove = Math.min(amountToRemove - removed, stackAmount);

                removed += toRemove;

                if (stackAmount <= toRemove)
                    human.getInventory().setItem(i, null);
                else
                    stack.setAmount(stackAmount - toRemove);
            }
        }
    }
}
