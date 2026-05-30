package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.Locale;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>(value)</td><td>String</td><td>Banner pattern tag key (e.g. minecraft:no_item_required, #minecraft:no_item_required)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "provides_banner_patterns")
public final class ProvidesBannerPatternsComponent extends ComponentExecutor {
    private @Nullable TagKey<PatternType> tagKey;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof String raw)) {
            Log.itemWarn("Components", namespacedID, "'provides_banner_patterns' must be a banner pattern tag key (e.g. minecraft:no_item_required)");
            return false;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("#")) key = key.substring(1);
        if (!key.contains(":")) key = "minecraft:" + key;
        this.tagKey = TagKey.create(RegistryKey.BANNER_PATTERN, Key.key(key));
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        if (tagKey != null) {
            itemStack.setData(DataComponentTypes.PROVIDES_BANNER_PATTERNS, tagKey);
        }
        return itemStack;
    }
}
