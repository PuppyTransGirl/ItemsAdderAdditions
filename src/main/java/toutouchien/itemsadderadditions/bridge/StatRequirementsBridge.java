package toutouchien.itemsadderadditions.bridge;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatRequirementsBridge {
    private StatRequirementsBridge() {}

    // true = requirements met, false = blocked
    private static final Map<UUID, Map<Integer, Boolean>> STATE =
            new ConcurrentHashMap<>();

    /**
     * Called from injected bytecode at the entry of ActionsLoader.a(Entity, ItemEventType, nq).
     *
     * @param actionsLoader the ActionsLoader instance (itemsadder/m/ActionsLoader)
     * @param entity        the Entity arg (arg 0 of the patched method)
     */
    public static void capture(Object actionsLoader, Object entity) {
        try {
            if (!(entity instanceof Player player)) return;

            // reflect xW field (InternalCustomStack)
            Field xwField = actionsLoader.getClass().getDeclaredField("xW");
            xwField.setAccessible(true);
            Object internalCustomStack = xwField.get(actionsLoader);
            if (internalCustomStack == null) return;

            // reflect checkStatRequirements(Player)
            Method checkMethod = internalCustomStack.getClass()
                    .getMethod("checkStatRequirements", Player.class);
            boolean result = (boolean) checkMethod.invoke(internalCustomStack, player);

            // derive the same hash key used by CooldownBridge
            Method getNamespacedId = internalCustomStack.getClass()
                    .getMethod("getNamespacedId");
            String namespacedId = (String) getNamespacedId.invoke(internalCustomStack);
            int itemHash = namespacedId.hashCode();

            STATE.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                 .put(itemHash, result);

        } catch (Exception ignored) {
            // if reflection fails we fail-open; the vanilla ItemsAdder check still runs
        }
    }

    /**
     * Returns {@code true} when stat requirements are NOT met (i.e. should block).
     */
    public static boolean isBlocked(Player player, int itemHash) {
        Map<Integer, Boolean> entry = STATE.get(player.getUniqueId());
        if (entry == null) return false;

        Boolean result = entry.get(itemHash);
        if (result == null) return false;

        return !result; // false = requirements not met = blocked
    }

    public static void clear(UUID playerId) {
        STATE.remove(playerId);
    }
}
