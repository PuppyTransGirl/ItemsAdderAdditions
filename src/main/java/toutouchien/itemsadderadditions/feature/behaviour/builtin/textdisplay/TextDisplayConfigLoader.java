package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayAlignment;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayBillboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@NullMarked
public final class TextDisplayConfigLoader {
    private static final String LOG_TAG = "TextDisplay";
    private static final Vector DEFAULT_OFFSET = new Vector(0.0, 0.25, 0.0);

    private TextDisplayConfigLoader() {
        throw new IllegalStateException("Utility class");
    }

    public static List<TextDisplaySpec> load(ConfigurationSection section, String namespacedId) {
        List<TextDisplaySpec> specs = new ArrayList<>();

        ConfigurationSection displays = section.getConfigurationSection("displays");
        if (displays != null) {
            for (String displayId : displays.getKeys(false)) {
                ConfigurationSection child = displays.getConfigurationSection(displayId);
                if (child == null) {
                    Log.warn(LOG_TAG, "text_display '{}' display '{}': display config must be a section; skipping.", namespacedId, displayId);
                    continue;
                }

                TextDisplaySpec spec = loadDisplay(child, namespacedId, displayId);
                if (spec != null) specs.add(spec);
            }
            return List.copyOf(specs);
        }

        if (!section.contains("text")) {
            Log.warn(LOG_TAG, "text_display '{}': missing 'text' or 'displays' section.", namespacedId);
            return List.of();
        }

        TextDisplaySpec spec = loadDisplay(section, namespacedId, "default");
        return spec == null ? List.of() : List.of(spec);
    }

    @Nullable
    private static TextDisplaySpec loadDisplay(ConfigurationSection section, String namespacedId, String displayId) {
        List<String> text = parseText(section.get("text"), namespacedId, displayId);
        if (text.isEmpty()) return null;

        Vector offset = parseOffset(section.get("offset"), namespacedId, displayId);
        PacketTextDisplayBillboard billboard = parseBillboard(section.getString("billboard"), namespacedId, displayId);
        PacketTextDisplayAlignment alignment = parseAlignment(section.getString("alignment"), namespacedId, displayId);
        boolean shadow = section.getBoolean("shadow", true);
        boolean seeThrough = section.getBoolean("see_through", false);
        int lineWidth = positiveInt(section, "line_width", 200, namespacedId, displayId);
        Integer background = TextDisplayColorParser.parse(section.get("background"), namespacedId, displayId);
        byte opacity = parseOpacity(section.get("opacity"), namespacedId, displayId);
        float scale = positiveFloat(section, "scale", 1.0F, namespacedId, displayId);
        double viewRange = positiveDouble(section, "view_range", 16.0D, namespacedId, displayId);
        int refreshInterval = nonNegativeInt(section, "refresh_interval", 0, namespacedId, displayId);

        return new TextDisplaySpec(
                displayId,
                text,
                offset,
                billboard,
                alignment,
                shadow,
                seeThrough,
                lineWidth,
                background,
                opacity,
                scale,
                viewRange,
                refreshInterval
        );
    }

    private static List<String> parseText(@Nullable Object raw, String namespacedId, String displayId) {
        if (raw instanceof String value) {
            if (value.isBlank()) {
                Log.warn(LOG_TAG, "text_display '{}' display '{}': text is blank; skipping display.", namespacedId, displayId);
                return List.of();
            }
            return List.of(value);
        }

        if (raw instanceof List<?> list) {
            List<String> lines = new ArrayList<>(list.size());
            for (Object value : list) {
                if (value instanceof String line) {
                    lines.add(line);
                } else {
                    Log.warn(LOG_TAG, "text_display '{}' display '{}': ignoring non-string text line '{}'.", namespacedId, displayId, value);
                }
            }

            if (lines.isEmpty()) {
                Log.warn(LOG_TAG, "text_display '{}' display '{}': text list has no valid string lines; skipping display.", namespacedId, displayId);
            }
            return List.copyOf(lines);
        }

        Log.warn(LOG_TAG, "text_display '{}' display '{}': text must be a string or list of strings; skipping display.", namespacedId, displayId);
        return List.of();
    }

    private static Vector parseOffset(@Nullable Object raw, String namespacedId, String displayId) {
        if (raw == null) return DEFAULT_OFFSET.clone();

        if (raw instanceof String value) {
            String[] split = value.split(",");
            if (split.length == 3) {
                try {
                    return new Vector(
                            Double.parseDouble(split[0].trim()),
                            Double.parseDouble(split[1].trim()),
                            Double.parseDouble(split[2].trim())
                    );
                } catch (NumberFormatException ignored) {
                    // fall through to warning
                }
            }
        }

        if (raw instanceof List<?> list && list.size() == 3) {
            try {
                return new Vector(number(list.get(0)), number(list.get(1)), number(list.get(2)));
            } catch (IllegalArgumentException ignored) {
                // fall through to warning
            }
        }

        Log.warn(LOG_TAG, "text_display '{}' display '{}': invalid offset '{}'; using 0,0.25,0.", namespacedId, displayId, raw);
        return DEFAULT_OFFSET.clone();
    }

    private static double number(@Nullable Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string.trim());
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("Not a number");
            }
        }
        throw new IllegalArgumentException("Not a number");
    }

    private static PacketTextDisplayBillboard parseBillboard(@Nullable String raw, String namespacedId, String displayId) {
        if (raw == null || raw.isBlank()) return PacketTextDisplayBillboard.VERTICAL;
        try {
            return PacketTextDisplayBillboard.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            Log.warn(LOG_TAG, "text_display '{}' display '{}': unknown billboard '{}'; using vertical.", namespacedId, displayId, raw);
            return PacketTextDisplayBillboard.VERTICAL;
        }
    }

    private static PacketTextDisplayAlignment parseAlignment(@Nullable String raw, String namespacedId, String displayId) {
        if (raw == null || raw.isBlank()) return PacketTextDisplayAlignment.CENTER;
        try {
            return PacketTextDisplayAlignment.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            Log.warn(LOG_TAG, "text_display '{}' display '{}': unknown alignment '{}'; using center.", namespacedId, displayId, raw);
            return PacketTextDisplayAlignment.CENTER;
        }
    }

    private static int positiveInt(ConfigurationSection section, String key, int fallback, String namespacedId, String displayId) {
        int value = section.getInt(key, fallback);
        if (value >= 1) return value;
        Log.warn(LOG_TAG, "text_display '{}' display '{}': '{}' must be >= 1; using {}.", namespacedId, displayId, key, fallback);
        return fallback;
    }

    private static int nonNegativeInt(ConfigurationSection section, String key, int fallback, String namespacedId, String displayId) {
        int value = section.getInt(key, fallback);
        if (value >= 0) return value;
        Log.warn(LOG_TAG, "text_display '{}' display '{}': '{}' must be >= 0; using {}.", namespacedId, displayId, key, fallback);
        return fallback;
    }

    private static double positiveDouble(ConfigurationSection section, String key, double fallback, String namespacedId, String displayId) {
        double value = section.getDouble(key, fallback);
        if (Double.isFinite(value) && value > 0.0D) return value;
        Log.warn(LOG_TAG, "text_display '{}' display '{}': '{}' must be positive; using {}.", namespacedId, displayId, key, fallback);
        return fallback;
    }

    private static float positiveFloat(ConfigurationSection section, String key, float fallback, String namespacedId, String displayId) {
        double value = section.getDouble(key, fallback);
        if (Double.isFinite(value) && value > 0.0D) return (float) value;
        Log.warn(LOG_TAG, "text_display '{}' display '{}': '{}' must be positive; using {}.", namespacedId, displayId, key, fallback);
        return fallback;
    }

    private static byte parseOpacity(@Nullable Object raw, String namespacedId, String displayId) {
        if (raw == null) return (byte) -1;

        int value;
        if (raw instanceof Number number) {
            value = number.intValue();
        } else if (raw instanceof String string) {
            try {
                value = Integer.parseInt(string.trim());
            } catch (NumberFormatException ex) {
                Log.warn(LOG_TAG, "text_display '{}' display '{}': opacity '{}' is not a number; using -1.", namespacedId, displayId, raw);
                return (byte) -1;
            }
        } else {
            Log.warn(LOG_TAG, "text_display '{}' display '{}': opacity must be a number; using -1.", namespacedId, displayId);
            return (byte) -1;
        }

        int clamped = Math.max(-1, Math.min(255, value));
        if (clamped != value) {
            Log.warn(LOG_TAG, "text_display '{}' display '{}': opacity {} is outside supported range -1..255; clamped to {}.", namespacedId, displayId, value, clamped);
        }
        return (byte) clamped;
    }
}
