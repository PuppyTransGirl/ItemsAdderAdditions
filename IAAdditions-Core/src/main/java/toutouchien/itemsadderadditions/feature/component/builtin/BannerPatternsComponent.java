package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BannerPatternLayers;
import net.kyori.adventure.key.Key;
import org.bukkit.DyeColor;
import org.bukkit.Registry;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
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
 * <tr><td>(list entry) type</td><td>String</td><td>Pattern type key (e.g. stripe_top, square_bottom_left)</td></tr>
 * <tr><td>(list entry) color</td><td>String</td><td>DyeColor name (e.g. RED, BLUE)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "banner_patterns")
public final class BannerPatternsComponent extends ComponentExecutor {
    private List<Pattern> patterns = List.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof List<?> rawList)) {
            Log.itemWarn("Components", namespacedID, "'banner_patterns' must be a list of sections with 'type' and 'color'");
            return false;
        }
        List<Pattern> parsed = new ArrayList<>();
        for (Object entry : rawList) {
            if (!(entry instanceof ConfigurationSection section)) continue;

            String rawType = section.getString("type", "");
            String rawColor = section.getString("color", "");

            String typeKey = rawType.toLowerCase(Locale.ROOT);
            if (!typeKey.contains(":")) typeKey = "minecraft:" + typeKey;
            PatternType patternType = Registry.BANNER_PATTERN.get(Key.key(typeKey));
            if (patternType == null) {
                Log.itemWarn("Components", namespacedID, "'banner_patterns' unknown pattern type '{}'.", rawType);
                return false;
            }

            DyeColor dyeColor;
            try {
                dyeColor = DyeColor.valueOf(rawColor.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                Log.itemWarn("Components", namespacedID, "'banner_patterns' invalid color '{}'.", rawColor);
                return false;
            }
            parsed.add(new Pattern(dyeColor, patternType));
        }
        if (parsed.isEmpty()) {
            Log.itemWarn("Components", namespacedID, "'banner_patterns' list must not be empty");
            return false;
        }
        this.patterns = List.copyOf(parsed);
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.BANNER_PATTERNS, BannerPatternLayers.bannerPatternLayers(patterns));
        return itemStack;
    }
}
