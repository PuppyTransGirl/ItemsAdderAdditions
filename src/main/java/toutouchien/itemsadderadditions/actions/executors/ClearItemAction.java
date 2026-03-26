package toutouchien.itemsadderadditions.actions.executors;

import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.utils.other.ItemUtils;
import toutouchien.itemsadderadditions.utils.other.NamespaceUtils;

/**
 * Removes a specific ItemsAdder item from the inventory of the target human entity.
 * <p>
 * Example:
 * <pre>{@code
 * clear_item:
 *   item:   "myitems:custom_gem" # Required - namespaced ID of the item to remove
 *   amount: 3 # Optional - how many to remove (default: 1)
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

    private String namespacedID;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        this.namespacedID = namespacedID;
        return true;
    }

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        if (!(runOn instanceof HumanEntity human))
            return;

        ItemStack itemStack = NamespaceUtils.itemByID(NamespaceUtils.namespace(namespacedID), item);
        if (itemStack == null)
            return;

        ItemUtils.removeItemsFromInventory(human, itemStack, amount);
    }
}
