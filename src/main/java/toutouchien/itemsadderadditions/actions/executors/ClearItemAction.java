package toutouchien.itemsadderadditions.actions.executors;

import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.ItemUtils;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;

/**
 * Removes a specific ItemsAdder item from the inventory of the target human entity.
 *
 * <p>Example:
 * <pre>{@code
 * clear_item:
 *   item:   "myitems:custom_gem"  # required - namespaced ID of the item to remove
 *   amount: 3                     # optional - how many to remove (default: 1)
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "clear_item")
public final class ClearItemAction extends ActionExecutor {
    @Parameter(key = "item", type = String.class, required = true)
    private String item;

    @Parameter(key = "amount", type = Integer.class)
    private int amount = 1;

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        if (!(runOn instanceof HumanEntity human))
            return;

        // Resolve the item relative to the namespace of the item that owns this action.
        // NamespaceUtils.itemByID accepts a full namespaced ID (e.g. "mypack:gem") directly.
        ItemStack itemStack = NamespaceUtils.itemByID(item, item);
        if (itemStack == null)
            return;

        ItemUtils.removeItemsFromInventory(human, itemStack, amount);
    }
}
