package toutouchien.itemsadderadditions.integration.hook.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.integration.hook.PluginHook;

import java.util.*;
import java.util.regex.Pattern;

@NullMarked
final class WorldGuardHook extends PluginHook implements WorldGuardDelegate {
    static final WorldGuardHook INSTANCE = new WorldGuardHook();

    private static final String LOG_TAG = "WorldGuard";
    private static final String ACTION_PREFIX = "iaa-action-";
    private static final Pattern ACTION_KEY = Pattern.compile("[a-z0-9][a-z0-9_-]*");
    private static final Pattern FLAG_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*");

    private final Map<String, StateFlag> flags = new LinkedHashMap<>();
    private final Map<String, StateFlag> actionFlags = new LinkedHashMap<>();
    private final Set<String> disabledFlags = new LinkedHashSet<>();
    private boolean registered;

    private WorldGuardHook() {
    }

    @Override
    public String pluginName() {
        return "WorldGuard";
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName()) && registered;
    }

    @Override
    public void registerFlags(JavaPlugin plugin, WorldGuardSettings settings, Collection<String> builtInActionKeys) {
        if (registered) return;

        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            for (WorldGuardFlagKey key : WorldGuardFlagKey.values()) {
                registerStateFlag(registry, key.flagName(), stateFlag -> flags.put(key.flagName(), stateFlag));
            }

            Set<String> actionKeys = new LinkedHashSet<>(builtInActionKeys);
            for (String actionKey : actionKeys) {
                registerActionFlag(registry, actionKey);
            }

            registered = true;
            info("Registered {} IAA WorldGuard flag(s).", flags.size() + actionFlags.size());
        } catch (RuntimeException ex) {
            warn("Failed to register IAA WorldGuard flags: {}", ex.getMessage());
        }
    }

    @Override
    public boolean test(Player player, Location location, WorldGuardFlagKey key) {
        StateFlag flag = flags.get(key.flagName());
        return flag == null || testState(player, location, flag);
    }

    @Override
    public boolean testAction(String actionKey, Player player, Location location) {
        String normalized = normalizeActionKey(actionKey);
        if (normalized == null) return true;

        StateFlag flag = actionFlags.get(normalized);
        return flag == null || testState(player, location, flag);
    }

    private boolean testState(Player player, Location location, StateFlag flag) {
        if (location.getWorld() == null) return true;

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionQuery query = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .createQuery();
        return query.testState(BukkitAdapter.adapt(location), localPlayer, flag);
    }

    private void registerActionFlag(FlagRegistry registry, String rawActionKey) {
        String normalized = normalizeActionKey(rawActionKey);
        if (normalized == null) {
            warn("Skipping invalid WorldGuard action flag key '{}'.", rawActionKey);
            return;
        }

        String flagName = ACTION_PREFIX + normalized.replace('_', '-');
        registerStateFlag(registry, flagName, flag -> actionFlags.put(normalized, flag));
    }

    private void registerStateFlag(FlagRegistry registry, String flagName, FlagConsumer consumer) {
        if (disabledFlags.contains(flagName)) return;

        StateFlag flag = new StateFlag(flagName, true);
        try {
            registry.register(flag);
            consumer.accept(flag);
        } catch (FlagConflictException conflict) {
            Flag<?> existing = registry.get(flagName);
            if (existing instanceof StateFlag stateFlag) {
                consumer.accept(stateFlag);
                warn("WorldGuard flag '{}' already exists - reusing compatible StateFlag.", flagName);
                return;
            }

            disabledFlags.add(flagName);
            warn("WorldGuard flag '{}' conflicts with non-StateFlag '{}'; this flag is disabled.",
                    flagName, existing == null ? "null" : existing.getClass().getSimpleName());
        }
    }

    private static @Nullable String normalizeActionKey(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return null;
        if (!ACTION_KEY.matcher(normalized).matches()) return null;

        String flagPart = normalized.replace('_', '-');
        return FLAG_NAME.matcher(flagPart).matches() ? normalized : null;
    }

    private static void info(String message, Object... args) {
        Bukkit.getLogger().info("[ItemsAdderAdditions/" + LOG_TAG + "] " + format(message, args));
    }

    private static void warn(String message, Object... args) {
        Bukkit.getLogger().warning("[ItemsAdderAdditions/" + LOG_TAG + "] " + format(message, args));
    }

    private static String format(String template, Object... args) {
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
            sb.append(args[argIdx++]);
            cursor = next + 2;
        }
        return sb.toString();
    }

    @FunctionalInterface
    private interface FlagConsumer {
        void accept(StateFlag flag);
    }
}
