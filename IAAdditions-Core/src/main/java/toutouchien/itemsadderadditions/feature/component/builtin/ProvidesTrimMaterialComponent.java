package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.Locale;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>(value)</td><td>String</td><td>Trim material key (e.g. gold, minecraft:gold)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused", "deprecation"})
@NullMarked
@Component(key = "provides_trim_material")
public final class ProvidesTrimMaterialComponent extends ComponentExecutor {
    private @Nullable TrimMaterial material;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof String raw)) {
            Log.itemWarn("Components", namespacedID, "'provides_trim_material' must be a trim material key (e.g. gold)");
            return false;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        if (!key.contains(":")) key = "minecraft:" + key;
        this.material = Registry.TRIM_MATERIAL.get(Key.key(key));
        if (this.material == null) {
            Log.itemWarn("Components", namespacedID, "'provides_trim_material' value '{}' is not a valid trim material.", raw);
            return false;
        }
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        if (material != null) {
            itemStack.setData(DataComponentTypes.PROVIDES_TRIM_MATERIAL, material);
        }
        return itemStack;
    }
}
