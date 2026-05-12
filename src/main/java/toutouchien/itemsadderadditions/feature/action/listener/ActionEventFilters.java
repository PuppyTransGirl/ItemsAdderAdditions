package toutouchien.itemsadderadditions.feature.action.listener;

import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.utils.PlayerUtils;

/**
 * Shared event filtering helpers for action listeners.
 */
@NullMarked
final class ActionEventFilters {
    private ActionEventFilters() {
    }

    @Nullable
    static String interactArgument(PlayerInteractEvent event) {
        boolean shift = event.getPlayer().isSneaking();
        return switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> shift ? "right_shift" : "right";
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> shift ? "left_shift" : "left";
            default -> null;
        };
    }

    static boolean ignoreOffHandDuplicate(PlayerInteractEvent event) {
        return PlayerUtils.isOffHandDuplicate(event.getPlayer(), event.getHand());
    }

    static boolean ignoreOffHandDuplicate(org.bukkit.entity.Player player, EquipmentSlot hand) {
        return PlayerUtils.isOffHandDuplicate(player, hand);
    }

    static boolean isInteractAllowed(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_AIR) {
            return true;
        }
        return event.useInteractedBlock() != Event.Result.DENY;
    }
}
