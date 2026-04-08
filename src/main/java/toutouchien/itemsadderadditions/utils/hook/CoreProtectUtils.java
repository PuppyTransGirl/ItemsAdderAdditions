package toutouchien.itemsadderadditions.utils.hook;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class CoreProtectUtils {
    private static TriState coreProtectLoaded = TriState.NOT_SET;
    @Nullable private static CoreProtectAPI cachedAPI = null;

    private CoreProtectUtils() {
        throw new IllegalStateException("Utility class");
    }

    @Nullable
    private static CoreProtectAPI getAPI() {
        if (coreProtectLoaded == TriState.FALSE)
            return null;

        if (coreProtectLoaded == TriState.NOT_SET) {
            coreProtectLoaded = TriState.byBoolean(
                    Bukkit.getPluginManager().isPluginEnabled("CoreProtect")
            );

            if (coreProtectLoaded == TriState.FALSE)
                return null;
        }

        // Resolve and cache the API instance on first use.
        if (cachedAPI == null) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("CoreProtect");
            if (!(plugin instanceof CoreProtect coreProtect)) {
                coreProtectLoaded = TriState.FALSE;
                return null;
            }

            CoreProtectAPI api = coreProtect.getAPI();
            if (api == null || !api.isEnabled()) {
                coreProtectLoaded = TriState.FALSE;
                return null;
            }

            cachedAPI = api;
        }

        return cachedAPI;
    }

    /**
     * Logs a container transaction to CoreProtect.
     *
     * <p><strong>Important:</strong> per the CoreProtect API contract, the actual inventory
     * modification must happen <em>immediately after</em> this call so that CoreProtect can
     * diff the before/after state correctly.
     *
     * @param user     The player name responsible for the change.
     * @param location The location of the container being modified.
     */
    public static void logContainerTransaction(String user, Location location) {
        CoreProtectAPI api = getAPI();
        if (api == null)
            return;

        api.logContainerTransaction(user, location);
    }
}
