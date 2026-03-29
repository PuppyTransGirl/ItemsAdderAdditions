package toutouchien.itemsadderadditions.utils.other;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PlaceholderAPIUtils {
    private static TriState placeholderAPILoaded = TriState.NOT_SET;

    private PlaceholderAPIUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String parsePlaceholders(@Nullable Player player, String text) {
        if (placeholderAPILoaded == TriState.NOT_SET) {
            placeholderAPILoaded = TriState.byBoolean(
                    Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
            );
        }

        return placeholderAPILoaded == TriState.TRUE
                ? PlaceholderAPI.setPlaceholders(player, text)
                : text;
    }
}
