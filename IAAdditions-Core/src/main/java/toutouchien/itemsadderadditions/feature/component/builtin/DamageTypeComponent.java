package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.damage.DamageType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.Locale;

/**
 * Sets the damage type of the item entity dropped by this item.
 *
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>(value)</td><td>String</td><td>Damage type registry key (e.g. minecraft:fire, minecraft:magic)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "damage_type")
public final class DamageTypeComponent extends ComponentExecutor {
    private @Nullable DamageType damageType;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof String raw)) {
            Log.itemWarn("Components", namespacedID, "'damage_type' must be a damage type key (e.g. minecraft:fire)");
            return false;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        if (!key.contains(":")) key = "minecraft:" + key;

        this.damageType = Registry.DAMAGE_TYPE.get(Key.key(key));
        if (this.damageType == null) {
            Log.itemWarn("Components", namespacedID, "'damage_type' value '{}' is not a known damage type.", raw);
            return false;
        }
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        if (damageType != null) {
            itemStack.setData(DataComponentTypes.DAMAGE_TYPE, damageType);
        }
        return itemStack;
    }
}
