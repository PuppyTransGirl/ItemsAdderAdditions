package toutouchien.itemsadderadditions.actions.executors;

import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;

import java.util.Locale;

/**
 * Swing mainhand or offhand.
 * <p>
 * Example:
 * <pre>{@code
 * swing_hand:
 *   hand: off_hand
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "swing_hand")
public class SwingHandAction extends ActionExecutor {
    @Parameter(key = "hand", type = String.class, required = true)
    private String hand;

    @Override
    protected void execute(ActionContext context) {
        EquipmentSlot equipmentSlot = EquipmentSlot.valueOf(hand.toUpperCase(Locale.ROOT));
        context.player().swingHand(equipmentSlot);
    }
}
