package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.DyeColor;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.Locale;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Values</th></tr>
 * <tr><td>(value)</td><td>String</td><td>WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME, PINK, GRAY,
 *                                        LIGHT_GRAY, CYAN, PURPLE, BLUE, BROWN, GREEN, RED, BLACK</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "base_color")
public final class BaseColorComponent extends ComponentExecutor {
    private @Nullable DyeColor color;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof String raw)) {
            Log.itemWarn("Components", namespacedID, "'base_color' must be a string (e.g. RED, BLUE)");
            return false;
        }
        try {
            this.color = DyeColor.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            Log.itemWarn("Components", namespacedID, "'base_color' value '{}' is not a valid DyeColor.", raw);
            return false;
        }
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        if (color != null) {
            itemStack.setData(DataComponentTypes.BASE_COLOR, color);
        }
        return itemStack;
    }
}
