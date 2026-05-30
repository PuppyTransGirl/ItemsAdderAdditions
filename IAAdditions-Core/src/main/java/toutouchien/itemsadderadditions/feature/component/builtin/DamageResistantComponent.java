package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DamageResistant;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.bukkit.damage.DamageType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.Locale;

/**
 * Makes the item entity immune to a tag of damage types (like netherite ignoring fire damage).
 *
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>(value)</td><td>String</td><td>Damage type tag key (e.g. minecraft:is_fire, #minecraft:is_fire)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "damage_resistant")
public final class DamageResistantComponent extends ComponentExecutor {
    private @Nullable TagKey<DamageType> tagKey;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof String raw)) {
            Log.itemWarn("Components", namespacedID, "'damage_resistant' must be a damage type tag key (e.g. minecraft:is_fire)");
            return false;
        }
        String stripped = raw.trim().toLowerCase(Locale.ROOT);
        if (stripped.startsWith("#")) {
            stripped = stripped.substring(1);
        }
        if (!stripped.contains(":")) {
            stripped = "minecraft:" + stripped;
        }
        try {
            this.tagKey = TagKey.create(RegistryKey.DAMAGE_TYPE, Key.key(stripped));
            return true;
        } catch (Exception e) {
            Log.itemWarn("Components", namespacedID, "'damage_resistant' value '{}' is not a valid tag key.", raw);
            return false;
        }
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        if (tagKey != null) {
            itemStack.setData(DataComponentTypes.DAMAGE_RESISTANT, DamageResistant.damageResistant(tagKey));
        }
        return itemStack;
    }
}
