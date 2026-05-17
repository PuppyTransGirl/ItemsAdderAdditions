package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

import java.util.Locale;

/**
 * Swings the player's main hand or off hand.
 * <p>
 * Example:
 * <pre>{@code
 * swing_hand:
 *   hand: "off_hand"   # HAND or OFF_HAND (case-insensitive)
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "swing_hand")
public final class SwingHandAction extends ActionExecutor {
    @Parameter(key = "hand", type = String.class, required = true)
    private String hand;

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        if (!(runOn instanceof LivingEntity livingEntity))
            return;

        EquipmentSlot slot;
        try {
            slot = EquipmentSlot.valueOf(hand.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return; // invalid value already warned at load time by ParameterInjector
        }

        livingEntity.swingHand(slot);
    }
}
