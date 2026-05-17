package toutouchien.itemsadderadditions.integration.hook;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class CoreProtectHook extends PluginHook {
    public static final CoreProtectHook INSTANCE = new CoreProtectHook();

    @Nullable private volatile CoreProtectAPI cachedAPI = null;
    private volatile boolean apiResolved = false;

    private CoreProtectHook() {
    }

    @Override
    public String pluginName() {
        return "CoreProtect";
    }

    @Nullable
    private CoreProtectAPI getAPI() {
        if (!isAvailable()) return null;

        if (apiResolved) return cachedAPI;
        synchronized (this) {
            if (!apiResolved) {
                Plugin plugin = Bukkit.getPluginManager().getPlugin("CoreProtect");
                if (plugin instanceof CoreProtect coreProtect) {
                    CoreProtectAPI api = coreProtect.getAPI();
                    if (api != null && api.isEnabled()) {
                        cachedAPI = api;
                    }
                }
                apiResolved = true;
            }
        }
        return cachedAPI;
    }

    /**
     * Logs a container transaction to CoreProtect.
     *
     * <p><strong>Important:</strong> per the CoreProtect API contract, the actual inventory
     * modification must happen <em>immediately after</em> this call so that CoreProtect can
     * diff the before/after state correctly.
     */
    public void logInteraction(String user, Location location) {
        CoreProtectAPI api = getAPI();
        if (api == null) return;
        api.logInteraction(user, location);
    }
}
