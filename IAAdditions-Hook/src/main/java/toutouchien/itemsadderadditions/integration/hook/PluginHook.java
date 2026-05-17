package toutouchien.itemsadderadditions.integration.hook;

import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;

/**
 * Base class for optional plugin integrations.
 *
 * <p>Subclasses declare which plugin they target via {@link #pluginName()} and call
 * {@link #isAvailable()} before delegating to that plugin's API. The availability
 * check is performed once and cached; subclasses do not need to manage that state.
 */
@NullMarked
public abstract class PluginHook {
    private volatile TriState available = TriState.NOT_SET;

    protected PluginHook() {
    }

    /**
     * The Bukkit plugin name to check for (as registered in plugin.yml).
     */
    public abstract String pluginName();

    /**
     * Returns {@code true} if the target plugin is currently installed and enabled.
     * The result is computed once on first call and cached for subsequent calls.
     */
    public final boolean isAvailable() {
        if (available != TriState.NOT_SET) return available == TriState.TRUE;
        synchronized (this) {
            if (available == TriState.NOT_SET) {
                available = TriState.byBoolean(
                        Bukkit.getPluginManager().isPluginEnabled(pluginName())
                );
            }
        }
        return available == TriState.TRUE;
    }
}
