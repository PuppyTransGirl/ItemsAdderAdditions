package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapDecorations;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCursor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>decoration_id</td><td>Section</td><td>type (MapCursor.Type), x (double), z (double), rotation (float)</td></tr>
 * </table>
 *
 * <pre>
 * map_decorations:
 *   my_marker:
 *     type: RED_MARKER
 *     x: 100.0
 *     z: 200.0
 *     rotation: 0.0
 * </pre>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "map_decorations")
public final class MapDecorationsComponent extends ComponentExecutor {
    private Map<String, MapDecorations.DecorationEntry> decorations = Map.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'map_decorations' must be a configuration section");
            return false;
        }
        Map<String, MapDecorations.DecorationEntry> parsed = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;

            String rawType = entry.getString("type", "player");
            String typeKey = rawType.trim().toLowerCase(Locale.ROOT);
            if (!typeKey.contains(":")) typeKey = "minecraft:" + typeKey;
            NamespacedKey typeNsKey = NamespacedKey.fromString(typeKey);
            MapCursor.Type cursorType = typeNsKey != null ? Registry.MAP_DECORATION_TYPE.get(typeNsKey) : null;
            if (cursorType == null) {
                Log.itemWarn("Components", namespacedID, "'map_decorations.{}.type' value '{}' is not a valid decoration type.", id, rawType);
                return false;
            }

            double x = entry.getDouble("x", 0.0);
            double z = entry.getDouble("z", 0.0);
            float rotation = (float) entry.getDouble("rotation", 0.0);

            parsed.put(id, MapDecorations.decorationEntry(cursorType, x, z, rotation));
        }
        if (parsed.isEmpty()) {
            Log.itemWarn("Components", namespacedID, "'map_decorations' section must not be empty");
            return false;
        }
        this.decorations = Map.copyOf(parsed);
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.MAP_DECORATIONS, MapDecorations.mapDecorations(decorations));
        return itemStack;
    }
}
