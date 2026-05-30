package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Enchantable;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Range</th></tr>
 * <tr><td>(value)</td><td>Integer</td><td>1 - 255</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "enchantable")
public final class EnchantableComponent extends ComponentExecutor {
    private int value = 1;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof Number n)) {
            Log.itemWarn("Components", namespacedID, "'enchantable' must be an integer (1 - 255)");
            return false;
        }

        int v = n.intValue();
        if (v < 1 || v > 255) {
            Log.itemWarn("Components", namespacedID, "'enchantable' value {} is out of range (1 - 255)", v);
            return false;
        }

        this.value = v;
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.ENCHANTABLE, Enchantable.enchantable(value));
        return itemStack;
    }
}
