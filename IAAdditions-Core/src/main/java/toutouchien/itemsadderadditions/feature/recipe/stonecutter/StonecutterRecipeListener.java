package toutouchien.itemsadderadditions.feature.recipe.stonecutter;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.feature.recipe.RecipeActions;

/**
 * Fires {@code on_complete} actions when a player takes from the stonecutter result slot.
 *
 * <p>The stonecutter inventory layout:
 * <ul>
 *   <li>Slot 0 - ingredient input</li>
 *   <li>Slot 1 - result output (raw slot 1)</li>
 * </ul>
 */
@NullMarked
public final class StonecutterRecipeListener implements Listener {
    private final StonecutterRecipeHandler handler;

    public StonecutterRecipeListener(StonecutterRecipeHandler handler) {
        this.handler = handler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        if (top.getType() != InventoryType.STONECUTTER) return;

        // Raw slot 1 is the stonecutter result slot.
        if (event.getRawSlot() != 1) return;

        @Nullable ItemStack result = top.getItem(1);
        if (result == null || result.getType().isAir()) return;

        @Nullable ItemStack ingredient = top.getItem(0);
        if (ingredient == null || ingredient.getType().isAir()) return;

        RecipeActions actions = handler.actionsFor(ingredient, result);
        if (actions == null || actions.isEmpty()) return;

        actions.execute(player);
    }
}
