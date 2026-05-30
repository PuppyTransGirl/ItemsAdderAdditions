package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapId;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th></tr>
 * <tr><td>(value)</td><td>Integer</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "map_id")
public final class MapIdComponent extends ComponentExecutor {
    private int id;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof Number n)) {
            Log.itemWarn("Components", namespacedID, "'map_id' must be an integer");
            return false;
        }

        this.id = n.intValue();
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.MAP_ID, MapId.mapId(id));
        return itemStack;
    }
}
