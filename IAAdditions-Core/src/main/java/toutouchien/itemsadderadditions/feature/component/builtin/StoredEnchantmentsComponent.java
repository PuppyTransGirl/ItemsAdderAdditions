package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
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
 * <tr><td>enchantment_key</td><td>Integer</td><td>Level (1 - 255) per enchantment key (e.g. sharpness: 5)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "stored_enchantments")
public final class StoredEnchantmentsComponent extends ComponentExecutor {
    private Map<Enchantment, Integer> enchantments = Map.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'stored_enchantments' must be a section of enchantment_key: level");
            return false;
        }
        Map<Enchantment, Integer> parsed = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            String k = key.toLowerCase(Locale.ROOT);
            if (!k.contains(":")) k = "minecraft:" + k;
            Enchantment ench = Registry.ENCHANTMENT.get(Key.key(k));
            if (ench == null) {
                Log.itemWarn("Components", namespacedID, "'stored_enchantments' unknown enchantment '{}'.", key);
                return false;
            }
            int level = section.getInt(key);
            if (level < 1 || level > 255) {
                Log.itemWarn("Components", namespacedID, "'stored_enchantments.{}' level {} is out of range (1 - 255).", key, level);
                return false;
            }
            parsed.put(ench, level);
        }
        if (parsed.isEmpty()) {
            Log.itemWarn("Components", namespacedID, "'stored_enchantments' section must not be empty");
            return false;
        }
        this.enchantments = Map.copyOf(parsed);
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantments.itemEnchantments(enchantments));
        return itemStack;
    }
}
