package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.SuspiciousStewEffects;
import io.papermc.paper.potion.SuspiciousEffectEntry;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
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
 * <tr><td>(list entry) type</td><td>String</td><td>Effect type key (e.g. minecraft:speed)</td></tr>
 * <tr><td>(list entry) duration</td><td>Integer</td><td>Duration in ticks (default: 160)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "suspicious_stew_effects")
public final class SuspiciousStewEffectsComponent extends ComponentExecutor {
    private List<SuspiciousEffectEntry> entries = List.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof List<?> rawList)) {
            Log.itemWarn("Components", namespacedID, "'suspicious_stew_effects' must be a list of sections with 'type' and 'duration'");
            return false;
        }
        List<SuspiciousEffectEntry> parsed = new ArrayList<>();
        for (Object entry : rawList) {
            if (!(entry instanceof ConfigurationSection section)) continue;

            String rawType = section.getString("type", "");
            String key = rawType.toLowerCase(Locale.ROOT);
            if (!key.contains(":")) key = "minecraft:" + key;

            PotionEffectType effectType = Registry.POTION_EFFECT_TYPE.get(Key.key(key));
            if (effectType == null) {
                Log.itemWarn("Components", namespacedID, "'suspicious_stew_effects' unknown effect type '{}'.", rawType);
                return false;
            }
            int duration = section.getInt("duration", 160);
            parsed.add(SuspiciousEffectEntry.create(effectType, duration));
        }
        if (parsed.isEmpty()) {
            Log.itemWarn("Components", namespacedID, "'suspicious_stew_effects' list must not be empty");
            return false;
        }
        this.entries = List.copyOf(parsed);
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.suspiciousStewEffects(entries));
        return itemStack;
    }
}
