package toutouchien.itemsadderadditions.integration.hook.worldguard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

@NullMarked
public final class WorldGuardProtection {
    private static final String LOG_TAG = "WorldGuard";
    private static @Nullable WorldGuardDelegate hook;
    private static boolean hookFailed;

    private WorldGuardProtection() {
    }

    public static void registerFlags(JavaPlugin plugin, WorldGuardSettings settings, Collection<String> builtInActionKeys) {
        WorldGuardDelegate active = hookIfInstalled();
        if (active == null) return;
        active.registerFlags(plugin, settings, builtInActionKeys);
    }

    public static boolean isAvailable() {
        WorldGuardDelegate active = hookIfInstalled();
        return active != null && active.isEnabled();
    }

    public static boolean isStorageOpenAllowed(Player player, Location location, WorldGuardSettings settings) {
        WorldGuardDelegate active = hookIfUsable(settings);
        return active == null || !settings.storageOpen() || active.test(player, location, WorldGuardFlagKey.STORAGE_OPEN);
    }

    public static boolean isContactDamageAllowed(Player player, Location location, WorldGuardSettings settings) {
        WorldGuardDelegate active = hookIfUsable(settings);
        return active == null || !settings.contactDamage() || active.test(player, location, WorldGuardFlagKey.CONTACT_DAMAGE);
    }

    public static boolean isStackablePlaceAllowed(Player player, Location location, WorldGuardSettings settings) {
        WorldGuardDelegate active = hookIfUsable(settings);
        return active == null || !settings.stackablePlace() || active.test(player, location, WorldGuardFlagKey.STACKABLE_PLACE);
    }

    public static boolean isBedUseAllowed(Player player, Location location, WorldGuardSettings settings) {
        WorldGuardDelegate active = hookIfUsable(settings);
        return active == null || !settings.bedUse() || active.test(player, location, WorldGuardFlagKey.BED_USE);
    }

    public static boolean isCustomPaintingPlaceAllowed(Player player, Location location, WorldGuardSettings settings) {
        WorldGuardDelegate active = hookIfUsable(settings);
        return active == null || !settings.customPaintingPlace() || active.test(player, location, WorldGuardFlagKey.CUSTOM_PAINTING_PLACE);
    }

    public static boolean isActionAllowed(String actionKey, Player player, Location location, WorldGuardSettings settings) {
        WorldGuardDelegate active = hookIfUsable(settings);
        return active == null || !settings.actions() || active.testAction(actionKey, player, location);
    }

    private static @Nullable WorldGuardDelegate hookIfUsable(WorldGuardSettings settings) {
        if (!settings.enabled()) return null;
        WorldGuardDelegate active = hookIfInstalled();
        return active != null && active.isEnabled() ? active : null;
    }

    private static @Nullable WorldGuardDelegate hookIfInstalled() {
        if (hookFailed) return null;
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return null;

        WorldGuardDelegate active = hook;
        if (active != null) return active;

        try {
            active = WorldGuardHook.INSTANCE;
            hook = active;
            return active;
        } catch (LinkageError error) {
            hookFailed = true;
            Bukkit.getLogger().warning("[ItemsAdderAdditions/" + LOG_TAG + "] WorldGuard plugin is present, but its API could not be loaded: " + error.getMessage());
            return null;
        }
    }
}
