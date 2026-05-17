package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.listener;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.BlockCoord;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageInventoryManager;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageRuntime;

@NullMarked
public final class StorageOpenVariantBlockListener implements Listener {
    private final StorageRuntime runtime;

    public StorageOpenVariantBlockListener(StorageRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Player right-clicks the open-form block. Open the GUI without transforming again.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpenVariantBlockInteract(PlayerInteractEvent event) {
        if (!runtime.hasBlockOpenVariant()) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getPlayer().isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        if (customBlock == null || !runtime.matchesOpenVariantId(customBlock.getNamespacedID())) return;
        if (!runtime.openVariantTransformer().isTransformed(block.getLocation())) return;

        event.setCancelled(true);
        runtime.sessionManager().openForPlayerAtTransformedLocation(
                event.getPlayer(),
                block.getLocation(),
                block,
                null
        );
    }

    /**
     * Pre-loads open-form block contents before CustomBlockData wipes them.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpenVariantBlockBreakPreLoad(BlockBreakEvent event) {
        if (!runtime.hasBlockOpenVariant()) return;

        Block block = event.getBlock();
        if (!runtime.openVariantTransformer().isTransformed(block.getLocation())) return;

        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        if (customBlock == null || !runtime.matchesOpenVariantId(customBlock.getNamespacedID())) return;

        // Prefer live GUI contents; fall back to stored block data.
        ItemStack[] live = runtime.sessionManager().getLiveContentsAt(block.getLocation());
        ItemStack[] contents = live != null
                ? live
                : StorageInventoryManager.loadFromBlock(
                block,
                runtime.contentsKey(),
                runtime.plugin()
        );

        runtime.preloadBlockContents(block, contents);
    }

    /**
     * The open-form block was broken: restore tracking and drop the original item.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpenVariantBlockBreak(CustomBlockBreakEvent event) {
        if (!runtime.hasBlockOpenVariant()) return;
        if (!runtime.matchesOpenVariantId(event.getNamespacedID())) return;

        Block block = event.getBlock();
        if (!runtime.openVariantTransformer().isTransformed(block.getLocation())) return;

        ItemStack[] contents = runtime.preloadedBlockContents()
                .remove(BlockCoord.of(block.getLocation()));

        runtime.sessionManager().closeSessionsAt(block.getLocation(), null);
        runtime.openVariantTransformer().forceRemove(block.getLocation());

        runtime.handleOpenVariantBreakDrops(block.getLocation(), contents);
        StorageInventoryManager.clearBlock(block, runtime.plugin());
    }
}
