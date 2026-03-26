package toutouchien.itemsadderadditions.utils.other;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

public class ItemUtils {
    public static int removeItemsFromInventory(HumanEntity human, ItemStack item, int amountToRemove) {
        if (human == null || item == null || amountToRemove <= 0) return 0;

        int removed = 0;
        ItemStack[] contents = human.getInventory().getContents();

        for (int i = 0; i < contents.length && removed < amountToRemove; i++) {
            ItemStack stack = contents[i];
            if (stack == null) continue;

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

        return removed;
    }
}
