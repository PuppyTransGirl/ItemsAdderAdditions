package toutouchien.itemsadderadditions.feature.action.listener;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionDispatcher;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

@NullMarked
public final class BlockActionListener implements Listener {
    private final ActionDispatcher dispatcher;

    public BlockActionListener(ActionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (ActionEventFilters.ignoreOffHandDuplicate(event)) return;

        ItemStack item = event.getItem();
        CustomStack customStack = item == null ? null : CustomStack.byItemStack(item);
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(event.getClickedBlock());
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();

        if (customStack != null) {
            dispatcher.dispatch(
                    customStack.getNamespacedID(),
                    TriggerType.BLOCK_INTERACT,
                    ActionContext.create(event.getPlayer(), TriggerType.BLOCK_INTERACT)
                            .block(event.getClickedBlock())
                            .heldItem(held)
                            .build()
            );
        }

        if (customBlock != null) {
            dispatcher.dispatch(
                    customBlock.getNamespacedID(),
                    TriggerType.BLOCK_INTERACT,
                    ActionContext.create(event.getPlayer(), TriggerType.BLOCK_INTERACT)
                            .block(event.getClickedBlock())
                            .heldItem(held)
                            .build()
            );
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(event.getBlock());
        if (customBlock != null) {
            dispatcher.dispatch(
                    customBlock.getNamespacedID(),
                    TriggerType.PLACED_BLOCK_BREAK,
                    ActionContext.create(player, TriggerType.PLACED_BLOCK_BREAK)
                            .block(event.getBlock())
                            .heldItem(tool)
                            .build()
            );
        }

        CustomStack customTool = CustomStack.byItemStack(tool);
        if (customTool == null) return;

        dispatcher.dispatch(
                customTool.getNamespacedID(),
                TriggerType.ITEM_BREAK_BLOCK,
                ActionContext.create(player, TriggerType.ITEM_BREAK_BLOCK)
                        .block(event.getBlock())
                        .heldItem(tool)
                        .build()
        );
    }
}
