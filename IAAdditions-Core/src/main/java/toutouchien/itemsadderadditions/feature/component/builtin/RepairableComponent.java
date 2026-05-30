package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Repairable;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>(value)</td><td>String or List&lt;String&gt;</td><td>Item type tag (e.g. #minecraft:planks) or list of item IDs</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "repairable")
public final class RepairableComponent extends ComponentExecutor {
    private io.papermc.paper.registry.set.@Nullable RegistryKeySet<ItemType> types;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (configData instanceof String raw) {
            String key = raw.trim().toLowerCase(Locale.ROOT);
            if (key.startsWith("#")) key = key.substring(1);
            if (!key.contains(":")) key = "minecraft:" + key;
            try {
                TagKey<ItemType> tagKey = TagKey.create(RegistryKey.ITEM, Key.key(key));
                this.types = Registry.ITEM.getTag(tagKey);
                if (this.types == null) {
                    Log.itemWarn("Components", namespacedID, "'repairable' tag '{}' not found.", raw);
                    return false;
                }
                return true;
            } catch (Exception e) {
                Log.itemWarn("Components", namespacedID, "'repairable' value '{}' is not a valid tag key.", raw);
                return false;
            }
        }

        if (configData instanceof List<?> rawList) {
            List<TypedKey<ItemType>> keys = new ArrayList<>();
            for (Object entry : rawList) {
                if (!(entry instanceof String s)) continue;
                String k = s.trim().toLowerCase(Locale.ROOT);
                if (!k.contains(":")) k = "minecraft:" + k;
                keys.add(TypedKey.create(RegistryKey.ITEM, Key.key(k)));
            }
            if (keys.isEmpty()) {
                Log.itemWarn("Components", namespacedID, "'repairable' list must not be empty");
                return false;
            }
            this.types = RegistrySet.keySet(RegistryKey.ITEM, keys);
            return true;
        }

        Log.itemWarn("Components", namespacedID, "'repairable' must be a tag string or list of item IDs");
        return false;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        if (types != null) {
            itemStack.setData(DataComponentTypes.REPAIRABLE, Repairable.repairable(types));
        }
        return itemStack;
    }
}
