package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.block.BlockPredicate;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAdventurePredicate;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.inventory.ItemStack;
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
 * <tr><td>(value)</td><td>List&lt;String&gt;</td><td>Block IDs or tags (e.g. minecraft:stone, #minecraft:logs)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "can_break")
public final class CanBreakComponent extends ComponentExecutor {
    private List<BlockPredicate> predicates = List.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof List<?> rawList)) {
            Log.itemWarn("Components", namespacedID, "'can_break' must be a list of block IDs or tags");
            return false;
        }
        List<BlockPredicate> parsed = new ArrayList<>();
        for (Object entry : rawList) {
            if (!(entry instanceof String raw)) continue;
            BlockPredicate predicate = parseBlockPredicate(raw.trim(), namespacedID, "can_break");
            if (predicate == null) return false;
            parsed.add(predicate);
        }
        if (parsed.isEmpty()) {
            Log.itemWarn("Components", namespacedID, "'can_break' list must not be empty");
            return false;
        }
        this.predicates = List.copyOf(parsed);
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.CAN_BREAK, ItemAdventurePredicate.itemAdventurePredicate(predicates));
        return itemStack;
    }

    static @Nullable BlockPredicate parseBlockPredicate(String raw, String namespacedID, String field) {
        String key = raw.toLowerCase(Locale.ROOT);
        if (key.startsWith("#")) {
            String tagKey = key.substring(1);
            if (!tagKey.contains(":")) tagKey = "minecraft:" + tagKey;
            try {
                io.papermc.paper.registry.set.RegistryKeySet<BlockType> tag =
                        Registry.BLOCK.getTag(TagKey.create(RegistryKey.BLOCK, Key.key(tagKey)));
                if (tag == null) {
                    Log.itemWarn("Components", namespacedID, "'{}' block tag '{}' not found.", field, raw);
                    return null;
                }
                return BlockPredicate.predicate().blocks(tag).build();
            } catch (Exception e) {
                Log.itemWarn("Components", namespacedID, "'{}' invalid block tag '{}'.", field, raw);
                return null;
            }
        }
        if (!key.contains(":")) key = "minecraft:" + key;
        return BlockPredicate.predicate()
                .blocks(RegistrySet.keySet(RegistryKey.BLOCK, TypedKey.create(RegistryKey.BLOCK, Key.key(key))))
                .build();
    }
}
