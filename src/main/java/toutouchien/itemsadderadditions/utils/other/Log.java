package toutouchien.itemsadderadditions.utils.other;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Centralized logger matching ItemsAdder's visual style.
 *
 * <p>Log lines follow the structure:
 * <pre>
 *   IAAdditions) [Subsystem] message
 * </pre>
 * <p>
 * Colors (taken directly from ItemsAdder's en.yml palette):
 * <ul>
 *   <li>Prefix bracket    - {@code #ac52d4} -> {@code #6c3484} gradient (purple)</li>
 *   <li>Closing {@code )} - {@code #999999} (gray)</li>
 *   <li>Subsystem tag     - {@code #4dd2ff} (light blue)</li>
 *   <li>Normal text       - {@code #ffffff} (white)</li>
 *   <li>Highlight         - {@code #ffe14d} (golden yellow, same as IA item names)</li>
 *   <li>Muted text        - {@code #999999} (gray)</li>
 *   <li>Success           - {@code #4dff4d} (bright green)</li>
 *   <li>Warning           - {@code #ffe14d} (golden yellow)</li>
 *   <li>Error             - {@code #ff4d4d} (red)</li>
 * </ul>
 */
@NullMarked
public final class Log {
    // ItemsAdder palette - taken verbatim from en.yml
    private static final TextColor C_PREFIX_START = TextColor.fromHexString("#AC52D4");
    private static final TextColor C_PREFIX_END = TextColor.fromHexString("#6C3484");
    private static final TextColor C_BRACKET = TextColor.fromHexString("#999999");
    private static final TextColor C_SUBSYSTEM = TextColor.fromHexString("#4DD2FF");
    private static final TextColor C_TEXT = TextColor.fromHexString("#FFFFFF");
    private static final TextColor C_HIGHLIGHT = TextColor.fromHexString("#FFE14D");
    private static final TextColor C_MUTED = TextColor.fromHexString("#999999");
    private static final TextColor C_SUCCESS = TextColor.fromHexString("#4DFF4D");
    private static final TextColor C_WARN = TextColor.fromHexString("#FFE14D");
    private static final TextColor C_ERROR = TextColor.fromHexString("#FF4D4D");

    private Log() {
        throw new IllegalStateException("Utility class");
    }

    public static void info(String subsystem, String message) {
        logger().info(line(subsystem, message, C_TEXT));
    }

    public static void info(String subsystem, String message, Object... args) {
        logger().info(line(subsystem, format(message, args), C_TEXT));
    }

    public static void success(String subsystem, String message, Object... args) {
        logger().info(line(subsystem, format(message, args), C_SUCCESS));
    }

    public static void warn(String subsystem, String message, Object... args) {
        logger().warn(line(subsystem, format(message, args), C_WARN));
    }

    public static void error(String subsystem, String message, Object... args) {
        logger().error(line(subsystem, format(message, args), C_ERROR));
    }

    public static void error(String subsystem, String message, Throwable cause) {
        logger().error(line(subsystem, message, C_ERROR), cause);
    }

    public static void debug(String subsystem, String message, Object... args) {
        logger().info(line(subsystem, format(message, args), C_TEXT));
    }

    /**
     * Warns about a config problem on a specific item.
     * <pre>IAAdditions) [Subsystem] Item 'ns:id' - message</pre>
     */
    public static void itemWarn(String subsystem, String itemId, String message, Object... args) {
        logger().warn(prefix(subsystem)
                .append(t("Item ", C_MUTED))
                .append(highlight(itemId))
                .append(t(" - " + format(message, args), C_WARN)));
    }

    /**
     * Reports that an item's executor was skipped.
     * <pre>IAAdditions) [Subsystem] Skipping 'ns:id': reason</pre>
     */
    public static void itemSkip(String subsystem, String itemId, String reason, Object... args) {
        logger().warn(prefix(subsystem)
                .append(t("Skipping ", C_MUTED))
                .append(highlight(itemId))
                .append(t(": " + format(reason, args), C_WARN)));
    }

    /**
     * Reports a successful load count.
     * <pre>IAAdditions) [Subsystem] Loaded N unit(s).</pre>
     */
    public static void loaded(String subsystem, int count, String unit) {
        logger().info(prefix(subsystem)
                .append(t("Loaded ", C_TEXT))
                .append(Component.text(count).color(C_HIGHLIGHT))
                .append(t(" " + unit + ".", C_TEXT)));
    }

    /**
     * Reports that an executor was registered.
     * <pre>IAAdditions) [Subsystem] Registered 'key'</pre>
     */
    public static void registered(String subsystem, String key) {
        logger().info(prefix(subsystem)
                .append(t("Registered ", C_MUTED))
                .append(highlight(key)));
    }

    /**
     * Reports that a built-in executor was skipped (disabled in config).
     * <pre>IAAdditions) [Subsystem] 'key' is disabled in config - skipping.</pre>
     */
    public static void disabled(String subsystem, String key) {
        logger().info(prefix(subsystem)
                .append(highlight(key))
                .append(t(" is disabled in config - skipping.", C_MUTED)));
    }

    private static Component line(String subsystem, String message, TextColor color) {
        return prefix(subsystem).append(t(message, color));
    }

    /**
     * Builds the {@code IAAdditions) [Subsystem] } prefix.
     *
     * <p>Matches ItemsAdder's own log prefix style:
     * purple-gradient bracket, gray closing paren, colored subsystem in brackets.
     */
    private static Component prefix(String subsystem) {
        Component bracket = gradientText("IAAdditions");
        Component paren = t(")", C_BRACKET);
        Component sub = t(" [" + subsystem + "]", C_SUBSYSTEM);
        return bracket.append(paren).append(sub).append(t(" ", C_TEXT));
    }

    /**
     * Renders a string as a linear two-stop gradient between the prefix colors.
     * Each character gets an interpolated color.
     */
    private static Component gradientText(String text) {
        return MiniMessage.miniMessage().deserialize("<gradient:%s:%s>%s</gradient>".formatted(C_PREFIX_START.asHexString(), C_PREFIX_END.asHexString(), text));
    }

    private static Component t(String value, TextColor color) {
        return Component.text(value).color(color);
    }

    private static Component highlight(String value) {
        return Component.text("'" + value + "'").color(C_HIGHLIGHT);
    }

    private static ComponentLogger logger() {
        return ComponentLogger.logger("");
    }

    /**
     * SLF4J-style {@code {}} placeholder substitution.
     */
    static String format(String template, @Nullable Object... args) {
        if (args.length == 0)
            return template;

        StringBuilder sb = new StringBuilder(template.length() + 32);
        int argIdx = 0;
        int cursor = 0;
        while (cursor < template.length()) {
            int next = template.indexOf("{}", cursor);
            if (next == -1 || argIdx >= args.length) {
                sb.append(template, cursor, template.length());
                break;
            }

            sb.append(template, cursor, next);
            Object arg = args[argIdx++];
            sb.append(arg == null ? "null" : arg);
            cursor = next + 2;
        }

        return sb.toString();
    }
}
