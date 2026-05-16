package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;

import java.util.Locale;

/**
 * Parses background color values from config into ARGB integers for the text display entity.
 */
@NullMarked
public final class TextDisplayColorParser {
    private static final String LOG_TAG = "TextDisplay";

    private TextDisplayColorParser() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Parses a background color from a config value.
     * <p>
     * Accepted formats: {@code #RRGGBB} (fully opaque), {@code #RRGGBBAA} (with alpha),
     * {@code false} or {@code null} string (no background). Returns {@code null} to indicate
     * that no background should be shown (fully transparent).
     */
    @Nullable
    public static Integer parse(@Nullable Object raw, String namespacedId, String displayId) {
        if (raw == null) return null;
        if (raw instanceof Boolean bool) {
            if (!bool) return null;
            Log.warn(LOG_TAG, "text_display '{}' display '{}': background=true is not supported; use #RRGGBB, #RRGGBBAA, false, or null.", namespacedId, displayId);
            return null;
        }

        if (!(raw instanceof String value)) {
            Log.warn(LOG_TAG, "text_display '{}' display '{}': background must be #RRGGBB, #RRGGBBAA, false, or null.", namespacedId, displayId);
            return null;
        }

        String color = value.trim();
        if (color.equalsIgnoreCase("false") || color.equalsIgnoreCase("null") || color.isEmpty()) {
            return null;
        }

        if (!color.startsWith("#") || (color.length() != 7 && color.length() != 9)) {
            Log.warn(LOG_TAG, "text_display '{}' display '{}': invalid background '{}'. Expected #RRGGBB or #RRGGBBAA.", namespacedId, displayId, value);
            return null;
        }

        String hex = color.substring(1).toUpperCase(Locale.ROOT);
        try {
            int rgb = Integer.parseUnsignedInt(hex.substring(0, 6), 16);
            int alpha = hex.length() == 8
                    ? Integer.parseUnsignedInt(hex.substring(6, 8), 16)
                    : 0xFF;
            return (alpha << 24) | rgb;
        } catch (NumberFormatException ex) {
            Log.warn(LOG_TAG, "text_display '{}' display '{}': invalid background '{}'. Expected hexadecimal color.", namespacedId, displayId, value);
            return null;
        }
    }
}
