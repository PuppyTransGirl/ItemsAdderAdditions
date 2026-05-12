package toutouchien.itemsadderadditions.integration.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Thin wrapper around PlaceholderAPI that safely no-ops when PlaceholderAPI
 * is not installed.
 *
 * <p>PlaceholderAPI availability is detected once on first use and cached.
 * The check is done lazily so that this class can be referenced safely even
 * before the server's plugin manager is fully initialized.
 */
@NullMarked
public final class PlaceholderAPIUtils {
    /**
     * {@code null} = not yet checked; {@code true/false} = checked result.
     * Volatile to ensure safe publication across threads.
     */
    @Nullable
    private static volatile Boolean placeholderAPIAvailable = null;

    private PlaceholderAPIUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Expands PlaceholderAPI placeholders in {@code text} for {@code player}.
     *
     * <p>When PlaceholderAPI is not installed, or when {@code player} is {@code null},
     * the original {@code text} is returned unchanged.
     *
     * @param player the player context for placeholder resolution, or {@code null}
     * @param text   the raw text that may contain {@code %placeholder%} tokens
     * @return the text with placeholders expanded, or the original text if PAPI is unavailable
     */
    public static String parsePlaceholders(@Nullable Player player, String text) {
        if (!isAvailable())
            return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    /**
     * Returns {@code true} if PlaceholderAPI is installed and enabled.
     * The result is cached after the first call.
     */
    private static boolean isAvailable() {
        if (placeholderAPIAvailable == null) {
            // Double-checked locking - safe because placeholderAPIAvailable is volatile.
            synchronized (PlaceholderAPIUtils.class) {
                if (placeholderAPIAvailable == null) {
                    placeholderAPIAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
                }
            }
        }
        return placeholderAPIAvailable;
    }
}

