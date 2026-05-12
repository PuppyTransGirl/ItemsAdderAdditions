package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.listener;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageInventoryManager;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageRuntime;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageType;

@NullMarked
public final class StorageBlockListener implements Listener {
    private final StorageRuntime runtime;

    public StorageBlockListener(StorageRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getPlayer().isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null || !runtime.matchesClosedBlock(block)) return;

        event.setCancelled(true);
        runtime.sessionManager().openForBlock(event.getPlayer(), block);
    }

    /**
     * Pre-loads block contents before CustomBlockData's MONITOR listener deletes them.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreakPreLoad(BlockBreakEvent event) {
        if (runtime.category() != ItemCategory.BLOCK) return;

        Block block = event.getBlock();

        // Let the open-variant listener deal with breaks on the open-form block.
        if (runtime.hasBlockOpenVariant()
                && runtime.openVariantTransformer().isTransformed(block.getLocation())) {
            CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
            if (customBlock != null && runtime.matchesOpenVariantId(customBlock.getNamespacedID())) return;
        }

        if (!runtime.matchesClosedBlock(block)) return;

        ItemStack[] contents = StorageInventoryManager.loadFromBlock(
                block,
                runtime.contentsKey(),
                runtime.plugin()
        );
        runtime.preloadBlockContents(block, contents);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(CustomBlockBreakEvent event) {
        if (!runtime.matchesClosedId(event.getNamespacedID())) return;

        Block block = event.getBlock();
        runtime.sessionManager().closeSessionsAt(
                block.getLocation(),
                runtime.preloadedBlockContents()
        );

        ItemStack[] contents = runtime.consumePreloadedBlockContents(block.getLocation());
        runtime.handleContainerBreak(block.getLocation(), contents);
        StorageInventoryManager.clearBlock(block, runtime.plugin());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(CustomBlockPlaceEvent event) {
        if (!runtime.matchesClosedId(event.getNamespacedID())) return;
        if (runtime.storageType() != StorageType.SHULKER) return;

        ItemStack[] stored = runtime.extractFromHand(event.getPlayer());
        if (stored == null) return;

        StorageInventoryManager.saveToBlock(
                event.getBlock(),
                stored,
                runtime.contentsKey(),
                runtime.plugin()
        );
    }
}
