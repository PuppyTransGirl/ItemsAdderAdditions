package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Fireworks;
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
 * <tr><td>flight_duration</td><td>Integer</td><td>1 (0 - 255)</td></tr>
 * <tr><td>explosions</td><td>List of sections</td><td>[] (same schema as firework_explosion)</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "fireworks")
public final class FireworksComponent extends ComponentExecutor {
    private int flightDuration = 1;
    private List<FireworkEffect> explosions = List.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'fireworks' must be a configuration section");
            return false;
        }
        int fd = section.getInt("flight_duration", 1);
        if (fd < 0 || fd > 255) {
            Log.itemWarn("Components", namespacedID, "'fireworks.flight_duration' {} is out of range (0 - 255).", fd);
            return false;
        }
        this.flightDuration = fd;

        List<?> rawExplosions = section.getList("explosions");
        if (rawExplosions != null) {
            List<FireworkEffect> parsed = new ArrayList<>();
            for (Object entry : rawExplosions) {
                if (!(entry instanceof ConfigurationSection expSection)) continue;

                String rawType = expSection.getString("type", "BALL");
                FireworkEffect.Type type;
                try {
                    type = FireworkEffect.Type.valueOf(rawType.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    Log.itemWarn("Components", namespacedID, "'fireworks.explosions' invalid type '{}'.", rawType);
                    return false;
                }

                List<Color> colors = FireworkExplosionComponent.parseColors(expSection.getList("colors"), namespacedID, "fireworks.explosions.colors");
                if (colors == null) return false;
                List<Color> fadeColors = FireworkExplosionComponent.parseColors(expSection.getList("fade_colors"), namespacedID, "fireworks.explosions.fade_colors");
                if (fadeColors == null) return false;

                FireworkEffect.Builder builder = FireworkEffect.builder().with(type)
                        .trail(expSection.getBoolean("trail", false))
                        .flicker(expSection.getBoolean("flicker", false));
                if (!colors.isEmpty()) builder.withColor(colors);
                if (!fadeColors.isEmpty()) builder.withFade(fadeColors);
                try {
                    parsed.add(builder.build());
                } catch (IllegalStateException e) {
                    Log.itemWarn("Components", namespacedID, "'fireworks.explosions' entry is invalid: {}", e.getMessage());
                    return false;
                }
            }
            this.explosions = List.copyOf(parsed);
        }
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.FIREWORKS, Fireworks.fireworks(explosions, flightDuration));
        return itemStack;
    }
}
