package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>(value)</td><td>String</td><td>#RRGGBB hex color</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "dyed_color")
public final class DyedColorComponent extends ComponentExecutor {
    private @Nullable Color color;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof String raw)) {
            Log.itemWarn("Components", namespacedID, "'dyed_color' must be a hex color string (#RRGGBB)");
            return false;
        }
        String hex = raw.trim();
        if (!hex.startsWith("#") || hex.length() != 7) {
            Log.itemWarn("Components", namespacedID, "'dyed_color' value '{}' is not a valid hex color. Use #RRGGBB.", raw);
            return false;
        }
        try {
            int rgb = Integer.parseUnsignedInt(hex.substring(1).toUpperCase(), 16);
            this.color = Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
            return true;
        } catch (NumberFormatException e) {
            Log.itemWarn("Components", namespacedID, "'dyed_color' value '{}' contains invalid hex digits.", raw);
            return false;
        }
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        if (color != null) {
            itemStack.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(color));
        }
        return itemStack;
    }
}
