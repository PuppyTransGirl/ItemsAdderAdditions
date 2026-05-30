package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotDecorations;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.Locale;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>back</td><td>String</td><td>null (vanilla item ID, e.g. brick or arms_up_pottery_sherd)</td></tr>
 * <tr><td>left</td><td>String</td><td>null</td></tr>
 * <tr><td>right</td><td>String</td><td>null</td></tr>
 * <tr><td>front</td><td>String</td><td>null</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "pot_decorations")
public final class PotDecorationsComponent extends ComponentExecutor {
    private @Nullable ItemType back;
    private @Nullable ItemType left;
    private @Nullable ItemType right;
    private @Nullable ItemType front;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'pot_decorations' must be a configuration section");
            return false;
        }
        this.back = resolveItemType(section.getString("back"), namespacedID, "back");
        this.left = resolveItemType(section.getString("left"), namespacedID, "left");
        this.right = resolveItemType(section.getString("right"), namespacedID, "right");
        this.front = resolveItemType(section.getString("front"), namespacedID, "front");
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.POT_DECORATIONS, PotDecorations.potDecorations(back, left, right, front));
        return itemStack;
    }

    private @Nullable ItemType resolveItemType(@Nullable String raw, String namespacedID, String field) {
        if (raw == null || raw.isBlank()) return null;
        String key = raw.trim().toLowerCase(Locale.ROOT);
        if (!key.contains(":")) key = "minecraft:" + key;
        ItemType type = Registry.ITEM.get(Key.key(key));
        if (type == null) {
            Log.itemWarn("Components", namespacedID, "'pot_decorations.{}' value '{}' is not a valid item type.", field, raw);
        }
        return type;
    }
}
