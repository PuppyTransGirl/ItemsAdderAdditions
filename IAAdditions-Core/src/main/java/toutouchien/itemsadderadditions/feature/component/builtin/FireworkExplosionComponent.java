package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
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
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>type</td><td>String</td><td>BALL (BALL, BALL_LARGE, STAR, BURST, CREEPER)</td></tr>
 * <tr><td>colors</td><td>List&lt;String&gt;</td><td>[] (#RRGGBB hex colors)</td></tr>
 * <tr><td>fade_colors</td><td>List&lt;String&gt;</td><td>[] (#RRGGBB hex colors)</td></tr>
 * <tr><td>trail</td><td>Boolean</td><td>false</td></tr>
 * <tr><td>flicker</td><td>Boolean</td><td>false</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "firework_explosion")
public final class FireworkExplosionComponent extends ComponentExecutor {
    private @Nullable FireworkEffect effect;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'firework_explosion' must be a configuration section");
            return false;
        }
        String rawType = section.getString("type", "BALL");
        FireworkEffect.Type type;
        try {
            type = FireworkEffect.Type.valueOf(rawType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Log.itemWarn("Components", namespacedID, "'firework_explosion.type' value '{}' is not valid. Use BALL, BALL_LARGE, STAR, BURST, or CREEPER.", rawType);
            return false;
        }

        List<Color> colors = parseColors(section.getList("colors"), namespacedID, "firework_explosion.colors");
        if (colors == null) return false;

        List<Color> fadeColors = parseColors(section.getList("fade_colors"), namespacedID, "firework_explosion.fade_colors");
        if (fadeColors == null) return false;

        boolean trail = section.getBoolean("trail", false);
        boolean flicker = section.getBoolean("flicker", false);

        FireworkEffect.Builder builder = FireworkEffect.builder().with(type).trail(trail).flicker(flicker);
        if (!colors.isEmpty()) builder.withColor(colors);
        if (!fadeColors.isEmpty()) builder.withFade(fadeColors);

        try {
            this.effect = builder.build();
        } catch (IllegalStateException e) {
            Log.itemWarn("Components", namespacedID, "'firework_explosion' is invalid: {}", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        if (effect != null) {
            itemStack.setData(DataComponentTypes.FIREWORK_EXPLOSION, effect);
        }
        return itemStack;
    }

    static @Nullable List<Color> parseColors(@Nullable List<?> rawList, String namespacedID, String field) {
        if (rawList == null) return List.of();
        List<Color> result = new ArrayList<>();
        for (Object entry : rawList) {
            if (!(entry instanceof String raw)) continue;
            String hex = raw.trim();
            if (!hex.startsWith("#") || hex.length() != 7) {
                Log.itemWarn("Components", namespacedID, "'{}' value '{}' is not a valid hex color. Use #RRGGBB.", field, raw);
                return null;
            }
            try {
                int rgb = Integer.parseUnsignedInt(hex.substring(1).toUpperCase(), 16);
                result.add(Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
            } catch (NumberFormatException e) {
                Log.itemWarn("Components", namespacedID, "'{}' value '{}' contains invalid hex digits.", field, raw);
                return null;
            }
        }
        return result;
    }
}
