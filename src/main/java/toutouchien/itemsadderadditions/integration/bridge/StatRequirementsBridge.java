package toutouchien.itemsadderadditions.integration.bridge;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatRequirementsBridge {
    // true = requirements met, false = blocked
    private static final Map<UUID, Map<Integer, Boolean>> STATE =
            new ConcurrentHashMap<>();

    private static Field internalCustomStackField;
    private static Method statCheckMethod;
    private static Method namespacedIdMethod;

    private StatRequirementsBridge() {
    }

    public static void capture(Object actionsLoader, Object entity) {
        try {
            if (!(entity instanceof Player player)) return;
            if (actionsLoader == null) return;

            Object internalCustomStack = resolveInternalCustomStack(actionsLoader);
            if (internalCustomStack == null) return;

            boolean result = (boolean) resolveStatCheckMethod(internalCustomStack)
                    .invoke(internalCustomStack, player);

            String namespacedId = (String) resolveNamespacedIdMethod(
                    internalCustomStack
            ).invoke(internalCustomStack);
            if (namespacedId == null) return;

            int itemHash = namespacedId.hashCode();

            STATE.computeIfAbsent(
                    player.getUniqueId(),
                    k -> new ConcurrentHashMap<>()
            ).put(itemHash, result);

        } catch (Exception ignored) {
            // fail-open; IA still does its own checks
        }
    }

    public static boolean isBlocked(Player player, int itemHash) {
        Map<Integer, Boolean> entry = STATE.get(player.getUniqueId());
        if (entry == null) return false;

        Boolean result = entry.get(itemHash);
        if (result == null) return false;

        return !result;
    }

    public static void clear(UUID playerId) {
        STATE.remove(playerId);
    }

    private static Object resolveInternalCustomStack(Object actionsLoader)
            throws ReflectiveOperationException {
        Field field = internalCustomStackField;
        if (field == null) {
            synchronized (StatRequirementsBridge.class) {
                field = internalCustomStackField;
                if (field == null) {
                    field = findField(actionsLoader.getClass(), "xW", "xI", "ya");
                    internalCustomStackField = field;
                }
            }
        }
        return field.get(actionsLoader);
    }

    private static Method resolveStatCheckMethod(Object internalCustomStack)
            throws ReflectiveOperationException {
        Method method = statCheckMethod;
        if (method == null) {
            synchronized (StatRequirementsBridge.class) {
                method = statCheckMethod;
                if (method == null) {
                    method = findMethod(
                            internalCustomStack.getClass(),
                            new String[]{
                                    "checkStatRequirements",
                                    "aT",
                                    "aV"
                            },
                            Player.class
                    );
                    statCheckMethod = method;
                }
            }
        }
        return method;
    }

    private static Method resolveNamespacedIdMethod(Object internalCustomStack)
            throws ReflectiveOperationException {
        Method method = namespacedIdMethod;
        if (method == null) {
            synchronized (StatRequirementsBridge.class) {
                method = namespacedIdMethod;
                if (method == null) {
                    method = findZeroArgMethod(
                            internalCustomStack.getClass(),
                            new String[]{"getNamespacedId", "getNamespacedID"}
                    );
                    namespacedIdMethod = method;
                }
            }
        }
        return method;
    }

    private static Field findField(Class<?> cls, String... names)
            throws NoSuchFieldException {
        for (Class<?> c = cls; c != null && c != Object.class;
             c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    Field field = c.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                }
            }
        }

        throw new NoSuchFieldException(
                "None of fields " + String.join(", ", names)
                        + " found in hierarchy of " + cls.getName()
        );
    }

    private static Method findMethod(
            Class<?> cls,
            String[] names,
            Class<?> paramType
    ) throws NoSuchMethodException {
        for (Class<?> c = cls; c != null && c != Object.class;
             c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (method.getParameterCount() != 1) continue;
                if (!method.getParameterTypes()[0].equals(paramType)) continue;

                for (String name : names) {
                    if (name.equals(method.getName())) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
        }

        throw new NoSuchMethodException(
                "None of methods " + String.join(", ", names)
                        + "(" + paramType.getName() + ") found in hierarchy of "
                        + cls.getName()
        );
    }

    private static Method findZeroArgMethod(Class<?> cls, String[] names)
            throws NoSuchMethodException {
        for (Class<?> c = cls; c != null && c != Object.class;
             c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (method.getParameterCount() != 0) continue;

                for (String name : names) {
                    if (name.equals(method.getName())) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
        }

        throw new NoSuchMethodException(
                "None of methods " + String.join(", ", names)
                        + "() found in hierarchy of " + cls.getName()
        );
    }
}
