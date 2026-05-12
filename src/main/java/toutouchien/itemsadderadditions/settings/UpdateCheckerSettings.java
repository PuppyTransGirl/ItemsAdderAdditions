package toutouchien.itemsadderadditions.settings;

import org.jspecify.annotations.NullMarked;

/**
 * Settings controlling the periodic Modrinth update check and join notification.
 */
@NullMarked
public record UpdateCheckerSettings(boolean enabled, boolean notifyOnJoin) {
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_NOTIFY_ON_JOIN = true;
}
