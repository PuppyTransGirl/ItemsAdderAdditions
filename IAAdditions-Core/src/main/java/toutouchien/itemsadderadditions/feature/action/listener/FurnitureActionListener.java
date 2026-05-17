package toutouchien.itemsadderadditions.feature.action.listener;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionDispatcher;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

@NullMarked
public final class FurnitureActionListener implements Listener {
    private static final String DEFAULT_INTERACT_ARGUMENT = "right";

    private final ActionDispatcher dispatcher;

    public FurnitureActionListener(ActionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        CustomFurniture furniture = event.getFurniture();
        if (furniture == null) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        dispatcher.dispatch(
                furniture.getNamespacedID(),
                TriggerType.FURNITURE_INTERACT,
                DEFAULT_INTERACT_ARGUMENT,
                ActionContext.create(player, TriggerType.FURNITURE_INTERACT)
                        .heldItem(held)
                        .eventArgument(DEFAULT_INTERACT_ARGUMENT)
                        .build()
        );
    }
}
