package toutouchien.itemsadderadditions.integration.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PlaceholderAPIHook extends PluginHook {
    public static final PlaceholderAPIHook INSTANCE = new PlaceholderAPIHook();

    private PlaceholderAPIHook() {
    }

    @Override
    public String pluginName() {
        return "PlaceholderAPI";
    }

    /**
     * Expands PlaceholderAPI placeholders in {@code text} for {@code player}.
     * Returns the original text unchanged when PlaceholderAPI is not installed.
     */
    public String parsePlaceholders(@Nullable Player player, String text) {
        if (!isAvailable()) return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}

