package toutouchien.itemsadderadditions.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlaceholderAPIUtils {
    private static TriState placeholderAPILoaded = TriState.NOT_SET;

    private PlaceholderAPIUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String parsePlaceholders(@Nullable Player player, String text) {
        if (placeholderAPILoaded != TriState.NOT_SET)
            return placeholderAPILoaded == TriState.TRUE ? PlaceholderAPI.setPlaceholders(player, text) : text;

        placeholderAPILoaded = TriState.byBoolean(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"));
        return parsePlaceholders(player, text);
    }
}
