package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.block.BlockPredicate;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAdventurePredicate;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>(value)</td><td>List&lt;String&gt;</td><td>Block IDs or tags (e.g. minecraft:stone, #minecraft:logs)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "can_place_on")
public final class CanPlaceOnComponent extends ComponentExecutor {
    private List<BlockPredicate> predicates = List.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof List<?> rawList)) {
            Log.itemWarn("Components", namespacedID, "'can_place_on' must be a list of block IDs or tags");
            return false;
        }
        List<BlockPredicate> parsed = new ArrayList<>();
        for (Object entry : rawList) {
            if (!(entry instanceof String raw)) continue;
            BlockPredicate predicate = CanBreakComponent.parseBlockPredicate(raw.trim(), namespacedID, "can_place_on");
            if (predicate == null) return false;
            parsed.add(predicate);
        }
        if (parsed.isEmpty()) {
            Log.itemWarn("Components", namespacedID, "'can_place_on' list must not be empty");
            return false;
        }
        this.predicates = List.copyOf(parsed);
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.CAN_PLACE_ON, ItemAdventurePredicate.itemAdventurePredicate(predicates));
        return itemStack;
    }
}
