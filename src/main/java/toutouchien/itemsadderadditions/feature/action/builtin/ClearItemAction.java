package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.common.utils.ItemUtils;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

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
