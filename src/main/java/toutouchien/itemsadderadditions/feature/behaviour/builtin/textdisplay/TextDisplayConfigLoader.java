package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayAlignment;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayBillboard;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayVisual;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses the {@code text_display} behaviour config section into a list of {@link TextDisplaySpec}s.
 * <p>
 * Supports both the shorthand single-display format (fields directly on the behaviour section)
 * and the multi-display format using a nested {@code displays} map.
 */
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

                TextDisplaySpec spec = loadDisplay(child, new LoadContext(namespacedId, displayId));
                if (spec != null) specs.add(spec);
            }
            return List.copyOf(specs);
        }

        if (!section.contains("text")) {
            Log.warn(LOG_TAG, "text_display '{}': missing 'text' or 'displays' section.", namespacedId);
            return List.of();
        }

        TextDisplaySpec spec = loadDisplay(section, new LoadContext(namespacedId, "default"));
        return spec == null ? List.of() : List.of(spec);
    }

    @Nullable
    private static TextDisplaySpec loadDisplay(ConfigurationSection section, LoadContext ctx) {
        List<String> text = parseText(section.get("text"), ctx);
        if (text.isEmpty()) return null;

        Vector offset = parseVector3(section.get("offset"), DEFAULT_OFFSET.clone(), "offset", ctx);
        PacketTextDisplayBillboard billboard = parseBillboard(section.getString("billboard"), ctx);
        PacketTextDisplayAlignment alignment = parseAlignment(section.getString("alignment"), ctx);
        boolean shadow = section.getBoolean("text_shadow", true);
        boolean seeThrough = section.getBoolean("see_through", false);
        int lineWidth = positiveInt(section, "line_width", 1403, ctx);
        Integer background = TextDisplayColorParser.parse(section.get("background"), ctx.namespacedId(), ctx.displayId());
        byte opacity = parseTextOpacity(section.get("text_opacity"), ctx);
        float[] scale = parseScale(section.get("scale"), ctx);
        double viewRange = positiveDouble(section, "view_range", 16.0D, ctx);
        int refreshInterval = nonNegativeInt(section, "refresh_interval", 0, ctx);
        int[] brightness = parseBrightness(section.get("brightness"), ctx);
        float shadowRadius = nonNegativeFloat(section, "shadow_radius", 0.0F, ctx);
        float shadowStrength = nonNegativeFloat(section, "shadow_strength", 1.0F, ctx);

        PacketTextDisplayVisual visual = new PacketTextDisplayVisual(
                billboard,
                alignment,
                shadow,
                seeThrough,
                lineWidth,
                background,
                opacity,
                scale[0],
                scale[1],
                scale[2],
                brightness == null ? null : brightness[0],
                brightness == null ? null : brightness[1],
                shadowRadius,
                shadowStrength
        );

        return new TextDisplaySpec(ctx.displayId(), text, offset, visual, viewRange, refreshInterval);
    }

    private static List<String> parseText(@Nullable Object raw, LoadContext ctx) {
        if (raw instanceof String value) {
            if (value.isBlank()) {
                Log.warn(LOG_TAG, "text_display '{}' display '{}': text is blank; skipping display.", ctx.namespacedId(), ctx.displayId());
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
                    Log.warn(LOG_TAG, "text_display '{}' display '{}': ignoring non-string text line '{}'.", ctx.namespacedId(), ctx.displayId(), value);
                }
            }

            if (lines.isEmpty()) {
                Log.warn(LOG_TAG, "text_display '{}' display '{}': text list has no valid string lines; skipping display.", ctx.namespacedId(), ctx.displayId());
            }
            return List.copyOf(lines);
        }

        Log.warn(LOG_TAG, "text_display '{}' display '{}': text must be a string or list of strings; skipping display.", ctx.namespacedId(), ctx.displayId());
        return List.of();
    }

    private static Vector parseVector3(@Nullable Object raw, Vector fallback, String key, LoadContext ctx) {
        if (raw == null) return fallback;

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
                return new Vector(toDouble(list.get(0)), toDouble(list.get(1)), toDouble(list.get(2)));
            } catch (IllegalArgumentException ignored) {
                // fall through to warning
            }
        }

        Log.warn(LOG_TAG, "text_display '{}' display '{}': invalid '{}' '{}'; expected \"x,y,z\" or [x, y, z]; using default.", ctx.namespacedId(), ctx.displayId(), key, raw);
        return fallback;
    }

    private static float[] parseScale(@Nullable Object raw, LoadContext ctx) {
        if (raw == null) return new float[]{1.0F, 1.0F, 1.0F};

        if (raw instanceof Number number) {
            float v = number.floatValue();
            if (v > 0.0F) return new float[]{v, v, v};
            Log.warn(LOG_TAG, "text_display '{}' display '{}': scale must be > 0; using 1.", ctx.namespacedId(), ctx.displayId());
            return new float[]{1.0F, 1.0F, 1.0F};
        }

        if (raw instanceof String value) {
            String[] split = value.split(",");
            if (split.length == 3) {
                try {
                    float x = Float.parseFloat(split[0].trim());
                    float y = Float.parseFloat(split[1].trim());
                    float z = Float.parseFloat(split[2].trim());
                    if (x > 0.0F && y > 0.0F && z > 0.0F) return new float[]{x, y, z};
                } catch (NumberFormatException ignored) {
                    // fall through to warning
                }
            }
        }

        if (raw instanceof List<?> list) {
            if (list.size() == 3) {
                try {
                    float x = (float) toDouble(list.get(0));
                    float y = (float) toDouble(list.get(1));
                    float z = (float) toDouble(list.get(2));
                    if (x > 0.0F && y > 0.0F && z > 0.0F) return new float[]{x, y, z};
                } catch (IllegalArgumentException ignored) {
                    // fall through to warning
                }
            } else if (list.size() == 1) {
                try {
                    float v = (float) toDouble(list.get(0));
                    if (v > 0.0F) return new float[]{v, v, v};
                } catch (IllegalArgumentException ignored) {
                    // fall through
                }
            }
        }

        Log.warn(LOG_TAG, "text_display '{}' display '{}': invalid scale '{}'; expected a positive number or [x, y, z] list; using 1.", ctx.namespacedId(), ctx.displayId(), raw);
        return new float[]{1.0F, 1.0F, 1.0F};
    }

    @Nullable
    private static int[] parseBrightness(@Nullable Object raw, LoadContext ctx) {
        if (raw == null) return null;

        if (raw instanceof Boolean bool) {
            if (!bool) return null;
            Log.warn(LOG_TAG, "text_display '{}' display '{}': brightness=true is not supported; use a 0-15 integer or [block, sky] list.", ctx.namespacedId(), ctx.displayId());
            return null;
        }

        if (raw instanceof Number number) {
            int v = Math.max(0, Math.min(15, number.intValue()));
            return new int[]{v, v};
        }

        if (raw instanceof List<?> list && list.size() == 2) {
            try {
                int block = Math.max(0, Math.min(15, (int) toDouble(list.get(0))));
                int sky = Math.max(0, Math.min(15, (int) toDouble(list.get(1))));
                return new int[]{block, sky};
            } catch (IllegalArgumentException ignored) {
                // fall through to warning
            }
        }

        if (raw instanceof ConfigurationSection section) {
            int block = Math.max(0, Math.min(15, section.getInt("block", 15)));
            int sky = Math.max(0, Math.min(15, section.getInt("sky", 15)));
            return new int[]{block, sky};
        }

        Log.warn(LOG_TAG, "text_display '{}' display '{}': invalid brightness '{}'; expected a 0-15 integer, [block, sky] list, or block:/sky: section; using world lighting.", ctx.namespacedId(), ctx.displayId(), raw);
        return null;
    }

    private static double toDouble(@Nullable Object value) {
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

    private static PacketTextDisplayBillboard parseBillboard(@Nullable String raw, LoadContext ctx) {
        if (raw == null || raw.isBlank()) return PacketTextDisplayBillboard.VERTICAL;
        try {
            return PacketTextDisplayBillboard.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            Log.warn(LOG_TAG, "text_display '{}' display '{}': unknown billboard '{}'; valid values: fixed, vertical, horizontal, center; using vertical.", ctx.namespacedId(), ctx.displayId(), raw);
            return PacketTextDisplayBillboard.VERTICAL;
        }
    }

    private static PacketTextDisplayAlignment parseAlignment(@Nullable String raw, LoadContext ctx) {
        if (raw == null || raw.isBlank()) return PacketTextDisplayAlignment.CENTER;
        try {
            return PacketTextDisplayAlignment.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            Log.warn(LOG_TAG, "text_display '{}' display '{}': unknown alignment '{}'; valid values: left, center, right; using center.", ctx.namespacedId(), ctx.displayId(), raw);
            return PacketTextDisplayAlignment.CENTER;
        }
    }

    private static byte parseTextOpacity(@Nullable Object raw, LoadContext ctx) {
        if (raw == null) return (byte) -1;

        float value;
        if (raw instanceof Number number) {
            value = number.floatValue();
        } else if (raw instanceof String string) {
            try {
                value = Float.parseFloat(string.trim());
            } catch (NumberFormatException ex) {
                Log.warn(LOG_TAG, "text_display '{}' display '{}': text_opacity '{}' is not a number; using 1.0 (fully opaque).", ctx.namespacedId(), ctx.displayId(), raw);
                return (byte) -1;
            }
        } else {
            Log.warn(LOG_TAG, "text_display '{}' display '{}': text_opacity must be a number; using 1.0 (fully opaque).", ctx.namespacedId(), ctx.displayId());
            return (byte) -1;
        }

        if (value >= 1.0f) return (byte) -1;
        return (byte) Math.round(Math.max(0.0f, value) * 254f);
    }

    private static int positiveInt(ConfigurationSection section, String key, int fallback, LoadContext ctx) {
        int value = section.getInt(key, fallback);
        if (value >= 1) return value;
        Log.warn(LOG_TAG, "text_display '{}' display '{}': '{}' must be >= 1; using {}.", ctx.namespacedId(), ctx.displayId(), key, fallback);
        return fallback;
    }

    private static int nonNegativeInt(ConfigurationSection section, String key, int fallback, LoadContext ctx) {
        int value = section.getInt(key, fallback);
        if (value >= 0) return value;
        Log.warn(LOG_TAG, "text_display '{}' display '{}': '{}' must be >= 0; using {}.", ctx.namespacedId(), ctx.displayId(), key, fallback);
        return fallback;
    }

    private static double positiveDouble(ConfigurationSection section, String key, double fallback, LoadContext ctx) {
        double value = section.getDouble(key, fallback);
        if (Double.isFinite(value) && value > 0.0D) return value;
        Log.warn(LOG_TAG, "text_display '{}' display '{}': '{}' must be positive; using {}.", ctx.namespacedId(), ctx.displayId(), key, fallback);
        return fallback;
    }

    private static float nonNegativeFloat(ConfigurationSection section, String key, float fallback, LoadContext ctx) {
        double value = section.getDouble(key, fallback);
        if (Double.isFinite(value) && value >= 0.0D) return (float) value;
        Log.warn(LOG_TAG, "text_display '{}' display '{}': '{}' must be >= 0; using {}.", ctx.namespacedId(), ctx.displayId(), key, fallback);
        return fallback;
    }

    private record LoadContext(String namespacedId, String displayId) {}
}
