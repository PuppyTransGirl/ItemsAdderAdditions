package toutouchien.itemsadderadditions.bridge;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public final class TradeMachineBridge {

    private static Object handlerInstance;

    private static Field registryField;
    private static Field sessionMapField;
    private static Field behaviorContainerField;
    private static Method lookupCustomItemMethod;
    private static Method behaviorLookupMethod;
    private static Method openMerchantMethod;

    private TradeMachineBridge() {
    }

    public static void capture(Object handler) {
        if (handlerInstance != null) return;
        handlerInstance = handler;
        Log.info(
                "TradeMachineBridge",
                "Trade-machine handler captured: " + handler.getClass().getName()
        );
    }

    public static boolean isReady() {
        return handlerInstance != null;
    }

    @SuppressWarnings("unchecked")
    public static boolean openTradeMachine(Player player, String namespacedId) {
        Object handler = handlerInstance;
        if (handler == null) {
            throw new IllegalStateException(
                    "Trade-machine handler has not been captured yet"
            );
        }

        CustomStack cs = CustomStack.getInstance(namespacedId);
        if (cs == null) return false;

        ItemStack itemStack = cs.getItemStack();

        try {
            Object registry = ensureRegistryField(handler).get(handler);

            Object customItemData = ensureLookupCustomItemMethod(registry)
                    .invoke(registry, itemStack);
            if (customItemData == null) return false;

            Object behaviorContainer = ensureBehaviorContainerField(customItemData)
                    .get(customItemData);

            Method lookupBehavior = ensureBehaviorLookupMethod(behaviorContainer);

            Object tradeMachine = lookupBehavior.invoke(
                    behaviorContainer, "furniture_trade_machine");
            if (tradeMachine == null) {
                tradeMachine = lookupBehavior.invoke(
                        behaviorContainer, "block_trade_machine");
            }
            if (tradeMachine == null) return false;

            ensureOpenMerchantMethod(tradeMachine).invoke(tradeMachine, player);

            ((Map<Player, Object>) ensureSessionMapField(handler)
                    .get(handler)).put(player, tradeMachine);

            return true;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to open trade machine via reflection: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private static Field ensureRegistryField(Object handler)
            throws NoSuchFieldException {
        if (registryField == null) {
            registryField = findField(handler.getClass(), "vG", "vK", "vs");
        }
        return registryField;
    }

    private static Field ensureSessionMapField(Object handler)
            throws NoSuchFieldException {
        if (sessionMapField == null) {
            sessionMapField = findField(handler.getClass(), "vH", "vL", "vt");
        }
        return sessionMapField;
    }

    private static Field ensureBehaviorContainerField(Object customItemData)
            throws NoSuchFieldException {
        if (behaviorContainerField == null) {
            behaviorContainerField = findField(
                    customItemData.getClass(), "BD", "BH", "Bm");
        }
        return behaviorContainerField;
    }

    private static Method ensureLookupCustomItemMethod(Object registry)
            throws NoSuchMethodException {
        if (lookupCustomItemMethod == null) {
            lookupCustomItemMethod = findMethod(
                    registry.getClass(),
                    new String[]{"a"},
                    "org.bukkit.inventory.ItemStack"
            );
        }
        return lookupCustomItemMethod;
    }

    private static Method ensureBehaviorLookupMethod(Object behaviorContainer)
            throws NoSuchMethodException {
        if (behaviorLookupMethod == null) {
            behaviorLookupMethod = findMethod(
                    behaviorContainer.getClass(),
                    new String[]{"bg", "bb"},
                    "java.lang.String"
            );
        }
        return behaviorLookupMethod;
    }

    private static Method ensureOpenMerchantMethod(Object tradeMachine)
            throws NoSuchMethodException {
        if (openMerchantMethod == null) {
            openMerchantMethod = findMethod(
                    tradeMachine.getClass(),
                    new String[]{"am", "ak"},
                    "org.bukkit.entity.Player"
            );
        }
        return openMerchantMethod;
    }

    private static Field findField(Class<?> cls, String... names)
            throws NoSuchFieldException {
        for (Class<?> c = cls; c != null && c != Object.class;
             c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    return accessible(c.getDeclaredField(name));
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
            String paramTypeName
    ) throws NoSuchMethodException {
        for (Class<?> c = cls; c != null && c != Object.class;
             c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!m.getParameterTypes()[0].getName().equals(paramTypeName)) {
                    continue;
                }

                for (String name : names) {
                    if (name.equals(m.getName())) {
                        return accessible(m);
                    }
                }
            }
        }

        throw new NoSuchMethodException(
                "None of methods " + String.join(", ", names)
                        + "(" + paramTypeName + ") found in hierarchy of "
                        + cls.getName()
        );
    }

    private static <T extends java.lang.reflect.AccessibleObject> T accessible(
            T obj
    ) {
        obj.setAccessible(true);
        return obj;
    }
}
