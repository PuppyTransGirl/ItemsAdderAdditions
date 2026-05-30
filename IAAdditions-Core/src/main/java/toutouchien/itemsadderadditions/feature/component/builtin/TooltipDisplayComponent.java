package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>hide_tooltip</td><td>Boolean</td><td>false</td></tr>
 * <tr><td>hidden_components</td><td>List&lt;String&gt;</td><td>[] (component keys, e.g. enchantments, attribute_modifiers)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "tooltip_display")
public final class TooltipDisplayComponent extends ComponentExecutor {
    private boolean hideTooltip = false;
    private List<DataComponentType> hiddenComponents = List.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'tooltip_display' must be a configuration section");
            return false;
        }
        this.hideTooltip = section.getBoolean("hide_tooltip", false);

        List<?> rawHidden = section.getList("hidden_components");
        if (rawHidden != null && !rawHidden.isEmpty()) {
            List<DataComponentType> resolved = new ArrayList<>();
            for (Object entry : rawHidden) {
                if (!(entry instanceof String key)) continue;
                DataComponentType type = resolveComponentType(key.trim().toUpperCase(Locale.ROOT));
                if (type == null) {
                    Log.itemWarn("Components", namespacedID,
                            "'tooltip_display.hidden_components' unknown component key '{}'.", key);
                    return false;
                }
                resolved.add(type);
            }
            this.hiddenComponents = List.copyOf(resolved);
        }
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        TooltipDisplay.Builder builder = TooltipDisplay.tooltipDisplay().hideTooltip(hideTooltip);
        if (!hiddenComponents.isEmpty()) {
            builder.addHiddenComponents(hiddenComponents.toArray(new DataComponentType[0]));
        }
        itemStack.setData(DataComponentTypes.TOOLTIP_DISPLAY, builder.build());
        return itemStack;
    }

    @Nullable
    private static DataComponentType resolveComponentType(String upperKey) {
        try {
            Field field = DataComponentTypes.class.getField(upperKey);
            Object value = field.get(null);
            return value instanceof DataComponentType dt ? dt : null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
