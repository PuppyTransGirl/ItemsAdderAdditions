package toutouchien.itemsadderadditions.feature.action.listener;

import dev.lone.itemsadder.api.CustomEntity;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionDispatcher;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

@NullMarked
public final class ComplexFurnitureActionListener implements Listener {
    private final ActionDispatcher dispatcher;

    public ComplexFurnitureActionListener(ActionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    private static CustomEntity findComplexFurnitureEntity(Block block) {
        for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 0.1, 0.1, 0.1)) {
            CustomEntity customEntity = CustomEntity.byAlreadySpawned(entity);
            if (customEntity != null) {
                return customEntity;
            }
        }
        return null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onComplexFurnitureInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARRIER) return;
        if (ActionEventFilters.ignoreOffHandDuplicate(event)) return;

        CustomEntity customEntity = findComplexFurnitureEntity(block);
        if (customEntity == null) return;

        Player player = event.getPlayer();
        Entity entity = customEntity.getEntity();
        ItemStack held = player.getInventory().getItemInMainHand();

        dispatcher.dispatch(
                customEntity.getNamespacedID(),
                TriggerType.COMPLEX_FURNITURE_INTERACT,
                ActionContext.create(player, TriggerType.COMPLEX_FURNITURE_INTERACT)
                        .complexFurniture(entity)
                        .heldItem(held)
                        .build()
        );
    }
}
