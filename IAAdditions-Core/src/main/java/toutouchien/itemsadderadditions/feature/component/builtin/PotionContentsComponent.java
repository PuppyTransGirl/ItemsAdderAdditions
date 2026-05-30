package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import net.kyori.adventure.key.Key;
import org.bukkit.Color;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.utils.PotionUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>potion</td><td>String</td><td>null (PotionType key, e.g. minecraft:strength)</td></tr>
 * <tr><td>color</td><td>String</td><td>null (#RRGGBB hex override)</td></tr>
 * <tr><td>custom_name</td><td>String</td><td>null</td></tr>
 * <tr><td>effects</td><td>List of sections</td><td>[] (type, duration, amplifier, ambient, particles, icon)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "potion_contents")
public final class PotionContentsComponent extends ComponentExecutor {
    private @Nullable PotionType potionType;
    private @Nullable Color color;
    private @Nullable String customName;
    private List<PotionEffect> effects = List.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'potion_contents' must be a configuration section");
            return false;
        }

        String rawPotion = section.getString("potion");
        if (rawPotion != null) {
            String key = rawPotion.trim().toLowerCase(Locale.ROOT);
            if (!key.contains(":")) key = "minecraft:" + key;
            this.potionType = Registry.POTION.get(Key.key(key));
            if (this.potionType == null) {
                Log.itemWarn("Components", namespacedID, "'potion_contents.potion' value '{}' is not a known potion type.", rawPotion);
                return false;
            }
        }

        String rawColor = section.getString("color");
        if (rawColor != null) {
            String hex = rawColor.trim();
            if (!hex.startsWith("#") || hex.length() != 7) {
                Log.itemWarn("Components", namespacedID, "'potion_contents.color' value '{}' is not a valid hex color.", rawColor);
                return false;
            }
            try {
                int rgb = Integer.parseUnsignedInt(hex.substring(1).toUpperCase(), 16);
                this.color = Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
            } catch (NumberFormatException e) {
                Log.itemWarn("Components", namespacedID, "'potion_contents.color' value '{}' contains invalid hex digits.", rawColor);
                return false;
            }
        }

        this.customName = section.getString("custom_name");

        List<?> rawEffects = section.getList("effects");
        if (rawEffects != null) {
            List<PotionEffect> parsed = new ArrayList<>();
            for (Object entry : rawEffects) {
                if (!(entry instanceof ConfigurationSection effectSection)) continue;
                PotionEffect effect = PotionUtils.parsePotion(effectSection);
                if (effect == null) {
                    Log.itemWarn("Components", namespacedID, "'potion_contents.effects' contains an invalid effect entry.");
                    return false;
                }
                parsed.add(effect);
            }
            this.effects = List.copyOf(parsed);
        }

        return potionType != null || color != null || customName != null || !effects.isEmpty();
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        PotionContents.Builder builder = PotionContents.potionContents()
                .potion(potionType)
                .customColor(color)
                .customName(customName);
        if (!effects.isEmpty()) builder.addCustomEffects(effects);
        itemStack.setData(DataComponentTypes.POTION_CONTENTS, builder.build());
        return itemStack;
    }
}
