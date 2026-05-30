package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.OminousBottleAmplifier;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Range</th></tr>
 * <tr><td>(value)</td><td>Integer</td><td>0 - 4</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "ominous_bottle_amplifier")
public final class OminousBottleAmplifierComponent extends ComponentExecutor {
    private int amplifier;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof Number n)) {
            Log.itemWarn("Components", namespacedID, "'ominous_bottle_amplifier' must be an integer (0 - 4)");
            return false;
        }

        int v = n.intValue();
        if (v < 0 || v > 4) {
            Log.itemWarn("Components", namespacedID, "'ominous_bottle_amplifier' value {} is out of range (0 - 4)", v);
            return false;
        }

        this.amplifier = v;
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, OminousBottleAmplifier.amplifier(amplifier));
        return itemStack;
    }
}
