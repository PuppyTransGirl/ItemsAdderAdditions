package toutouchien.itemsadderadditions.feature.action.listener;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
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

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        ItemStack item = event.getItem();
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();

        dispatchItemId(
                NamespaceUtils.itemID(item),
                event.getPlayer(),
                clicked,
                held
        );

        dispatchBlockId(
                NamespaceUtils.blockID(clicked),
                event.getPlayer(),
                clicked,
                held
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        dispatcher.dispatch(
                NamespaceUtils.blockID(event.getBlock()),
                TriggerType.PLACED_BLOCK_BREAK,
                ActionContext.create(player, TriggerType.PLACED_BLOCK_BREAK)
                        .block(event.getBlock())
                        .heldItem(tool)
                        .build()
        );

        String toolId = NamespaceUtils.itemID(tool);
        if (toolId == null) return;

        dispatcher.dispatch(
                toolId,
                TriggerType.ITEM_BREAK_BLOCK,
                ActionContext.create(player, TriggerType.ITEM_BREAK_BLOCK)
                        .block(event.getBlock())
                        .heldItem(tool)
                        .build()
        );
    }

    private void dispatchItemId(@Nullable String itemId, Player player, Block clicked, ItemStack held) {
        if (itemId == null) return;
        dispatcher.dispatch(
                itemId,
                TriggerType.BLOCK_INTERACT,
                ActionContext.create(player, TriggerType.BLOCK_INTERACT)
                        .block(clicked)
                        .heldItem(held)
                        .build()
        );
    }

    private void dispatchBlockId(String blockId, Player player, Block clicked, ItemStack held) {
        dispatcher.dispatch(
                blockId,
                TriggerType.BLOCK_INTERACT,
                ActionContext.create(player, TriggerType.BLOCK_INTERACT)
                        .block(clicked)
                        .heldItem(held)
                        .build()
        );
    }
}
