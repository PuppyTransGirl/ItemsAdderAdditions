package toutouchien.itemsadderadditions.common.utils;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * Stateless utility methods for common player-level queries.
 *
 * <p>All methods are pure functions with no side effects - safe to call from
 * any thread or event handler.
 */
@NullMarked
public final class PlayerUtils {
    private PlayerUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns {@code true} when this off-hand event should be ignored because the
     * same interaction is already being handled by the main-hand path.
     *
     * <p>We only suppress the off-hand event when the main hand already holds a
     * custom item. This preserves off-hand-only custom item interactions while
     * preventing duplicate execution for items held in the main hand.
     *
     * @param player the interacting player
     * @param hand   the hand that triggered the event
     * @return {@code true} if the event should be skipped
     */
    public static boolean isOffHandDuplicate(Player player, EquipmentSlot hand) {
        if (hand != EquipmentSlot.OFF_HAND) return false;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return !mainHand.isEmpty() && CustomStack.byItemStack(mainHand) != null;
    }
}
