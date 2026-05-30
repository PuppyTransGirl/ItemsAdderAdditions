package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Range</th></tr>
 * <tr><td>(value)</td><td>Float</td><td>0.0 - 255.0</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "potion_duration_scale")
public final class PotionDurationScaleComponent extends ComponentExecutor {
    private float scale;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof Number n)) {
            Log.itemWarn("Components", namespacedID, "'potion_duration_scale' must be a float (0.0 - 255.0)");
            return false;
        }

        float v = n.floatValue();
        if (v < 0f || v > 255f) {
            Log.itemWarn("Components", namespacedID, "'potion_duration_scale' value {} is out of range (0.0 - 255.0)", v);
            return false;
        }

        this.scale = v;
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.POTION_DURATION_SCALE, scale);
        return itemStack;
    }
}
