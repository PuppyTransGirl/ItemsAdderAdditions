package toutouchien.itemsadderadditions.utils.other;

import toutouchien.itemsadderadditions.ItemsAdderAdditions;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small shared console logger used by the plugin subsystems.
 */
public final class Log {
    private static volatile boolean debug;

    private Log() {
    }

    public static boolean debug() {
        return debug;
    }

    public static void toggleDebug() {
        debug = !debug;
        info("Debug", "Debug logging {}", debug ? "enabled" : "disabled");
    }

    public static void debug(String tag, String message, Object... args) {
        if (!debug) return;
        log(Level.INFO, tag, message, args);
    }

    public static void info(String tag, String message, Object... args) {
        log(Level.INFO, tag, message, args);
    }

    public static void success(String tag, String message, Object... args) {
        log(Level.INFO, tag, message, args);
    }

    public static void warn(String tag, String message, Object... args) {
        log(Level.WARNING, tag, message, args);
    }

    public static void error(String tag, String message, Object... args) {
        log(Level.SEVERE, tag, message, args);
    }

    public static void registered(String prefix, String key) {
        debug(prefix, "Registered '{}'", key);
    }

    public static void disabled(String prefix, String key) {
        debug(prefix, "Disabled '{}'", key);
    }

    public static void loaded(String subsystem, int total, String unit) {
        info(subsystem, "Loaded {} {}", total, unit);
    }

    public static void itemSkip(String subsystem, String itemName, String message, Object... args) {
        debug(subsystem, "Skipping {}: {}", itemName, format(message, args));
    }

    public static void itemWarn(String subsystem, String itemName, String message, Object... args) {
        warn(subsystem, "{}: {}", itemName, format(message, args));
    }

    private static void log(Level level, String tag, String message, Object... args) {
        Throwable throwable = null;
        Object[] formatArgs = args;
        if (args != null && args.length > 0 && args[args.length - 1] instanceof Throwable last) {
            throwable = last;
            formatArgs = new Object[args.length - 1];
            System.arraycopy(args, 0, formatArgs, 0, args.length - 1);
        }

        Logger logger = logger();
        String formatted = "[" + tag + "] " + format(message, formatArgs);
        if (throwable == null) {
            logger.log(level, formatted);
        } else {
            logger.log(level, formatted, throwable);
        }
    }

    private static Logger logger() {
        ItemsAdderAdditions plugin = ItemsAdderAdditions.instance();
        return plugin == null ? Logger.getLogger("ItemsAdderAdditions") : plugin.getLogger();
    }

    private static String format(String message, Object... args) {
        if (message == null) return "null";
        if (args == null || args.length == 0) return message;

        StringBuilder out = new StringBuilder(message.length() + args.length * 8);
        int argIndex = 0;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '{' && i + 1 < message.length() && message.charAt(i + 1) == '}' && argIndex < args.length) {
                out.append(String.valueOf(args[argIndex++]));
                i++;
            } else {
                out.append(c);
            }
        }
        while (argIndex < args.length) {
            out.append(' ').append(String.valueOf(args[argIndex++]));
        }
        return out.toString();
    }
}
