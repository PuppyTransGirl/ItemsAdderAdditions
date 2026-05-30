package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.version.VersionUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <p><strong>Minimum Minecraft Version:</strong> 1.21.1
 *
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Range</th></tr>
 * <tr><td>(value)</td><td>Float</td><td>0.0 - 1.0</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "minimum_attack_charge")
public final class MinimumAttackChargeComponent extends ComponentExecutor {
    private float charge;

    @Override
    public @Nullable VersionUtils minimumVersion() {
        return VersionUtils.v1_21_1;
    }

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof Number n)) {
            Log.itemWarn("Components", namespacedID, "'minimum_attack_charge' must be a float (0.0 - 1.0)");
            return false;
        }

        float v = n.floatValue();
        if (v < 0f || v > 1f) {
            Log.itemWarn("Components", namespacedID, "'minimum_attack_charge' value {} is out of range (0.0 - 1.0)", v);
            return false;
        }

        this.charge = v;
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.MINIMUM_ATTACK_CHARGE, charge);
        return itemStack;
    }
}
