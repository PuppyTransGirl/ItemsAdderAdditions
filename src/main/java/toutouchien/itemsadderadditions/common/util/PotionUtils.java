package toutouchien.itemsadderadditions.common.util;

import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

@NullMarked
public final class PotionUtils {
    private PotionUtils() {
        throw new IllegalStateException("Utility class");
    }

    @Nullable
    public static PotionEffect parsePotion(@Nullable ConfigurationSection section) {
        if (section == null)
            return null;

        String type = section.getString("type");
        if (type == null || type.isBlank())
            return null;

        int duration = section.getInt("duration", 40);
        int amplifier = section.getInt("amplifier", 1);
        boolean ambient = section.getBoolean("ambient", false);
        boolean particles = section.getBoolean("particles", true);
        boolean icon = section.getBoolean("icon", true);

        PotionEffectType effectType = Registry.POTION_EFFECT_TYPE.get(
                Key.key("minecraft", type.toLowerCase(Locale.ROOT))
        );
        if (effectType == null)
            return null;

        return new PotionEffect(effectType, duration, amplifier, ambient, particles, icon);
    }
}
