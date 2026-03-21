package toutouchien.itemsadderadditions.utils.other;

import io.papermc.paper.datacomponent.item.PotionContents;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

public class PotionUtils {
    @Nullable
    public static PotionEffect parsePotion(@Nullable ConfigurationSection section) {
        if (section == null)
            return null;

        String type = section.getString("type").toLowerCase(Locale.ROOT);
        int duration = section.getInt("duration", 40);
        int amplifier = section.getInt("amplifier", 1);
        boolean ambient = section.getBoolean("ambient", false);
        boolean particles = section.getBoolean("particles", true);
        boolean icon = section.getBoolean("icon", true);

        return new PotionEffect(
                Registry.POTION_EFFECT_TYPE.get(Key.key("minecraft", type)),
                duration,
                amplifier,
                ambient,
                particles,
                icon
        );
    }
}
