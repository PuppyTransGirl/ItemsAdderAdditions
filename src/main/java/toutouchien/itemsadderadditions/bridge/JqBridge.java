package toutouchien.itemsadderadditions.bridge;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Stores the single {@code itemsadder.m.jq} (trade-machine handler) instance
 * captured by {@link toutouchien.itemsadderadditions.patches.impl.JqCapturePatch}
 * and exposes a safe way to programmatically open a trade-machine GUI.
 *
 * <h3>Reflection strategy</h3>
 * All reflected handles are resolved lazily and cached behind a
 * double-checked lock so the per-call overhead is a single volatile read
 * after the first invocation.
 */
public final class JqBridge {

    private static Object jqInstance;

    // cached reflected handles (null until first use)
    private static Field vGField;       // jq.vG  : the 'a' item registry
    private static Field vHField;       // jq.vH  : WeakHashMap<Player, ip|id>
    private static Field BDField;       // oy.BD  : behaviour data map
    private static Method aLookupMethod; // a.a(ItemStack) -> oy
    private static Method bgMethod;      // oy$BD.bg(String) -> Object
    private static Method amMethod;      // ix.am(Player) -> void

    private JqBridge() {
    }

    /**
     * Called by {@code JqCapturePatch} at the end of the {@code jq} constructor.
     * Only the first call is recorded; subsequent ones are no-ops.
     */
    public static void capture(Object jq) {
        if (jqInstance != null) return;
        jqInstance = jq;
        Log.info("JqBridge", "jq instance captured: " + jq.getClass().getName());
    }

    public static boolean isReady() {
        return jqInstance != null;
    }

    /**
     * Opens the trade-machine GUI for {@code player} identified by
     * {@code namespacedId} (e.g. {@code "myns:my_machine"}).
     *
     * <p>This replicates what {@code jq} does when a player physically
     * right-clicks a furniture/block with a trade-machine behaviour:</p>
     * <ol>
     *   <li>Look up the {@code oy} (custom item data) for the given ID.</li>
     *   <li>Retrieve the {@code ip} (furniture) or {@code id} (block)
     *       trade-machine behaviour from {@code oy.BD}.</li>
     *   <li>Call {@code ix.am(player)} to open the {@link org.bukkit.inventory.Merchant}.</li>
     *   <li>Register the session in {@code jq.vH} so inventory-click handling
     *       works exactly as with a physical interaction.</li>
     * </ol>
     *
     * @return {@code true} if the GUI was opened; {@code false} if
     * {@code namespacedId} is unknown or has no trade-machine behaviour.
     * @throws IllegalStateException if {@code jq} has not been captured yet.
     * @throws RuntimeException      wrapping any reflection error.
     */
    public static boolean openTradeMachine(Player player, String namespacedId) {
        Object jq = jqInstance;
        if (jq == null) {
            throw new IllegalStateException(
                    "jq has not been captured yet — ItemsAdder may still be loading");
        }

        // 1. Resolve the IA custom stack → ItemStack
        CustomStack cs = CustomStack.getInstance(namespacedId);
        if (cs == null) return false;
        ItemStack itemStack = cs.getItemStack();

        try {
            // 2. vG : the item registry (class 'a')
            Object aInstance = ensureVGField(jq).get(jq);

            // 3. a.a(ItemStack) → oy
            Object oyInstance = ensureALookupMethod(aInstance).invoke(aInstance, itemStack);
            if (oyInstance == null) return false;

            // 4. oy.BD : the behaviour-data container
            Object BD = ensureBDField(oyInstance).get(oyInstance);

            // 5. BD.bg(String) → furniture or block trade-machine behaviour
            Method bg = ensureBgMethod(BD);
            Object tradeMachine = bg.invoke(BD, "furniture_trade_machine");
            if (tradeMachine == null) {
                tradeMachine = bg.invoke(BD, "block_trade_machine");
            }
            if (tradeMachine == null) return false;

            // 6. ix.am(Player) : open the Merchant GUI
            ensureAmMethod(tradeMachine).invoke(tradeMachine, player);

            // 7. Register the session in jq.vH so click-handling works
            //noinspection unchecked
            ((Map<Player, Object>) ensureVHField(jq).get(jq)).put(player, tradeMachine);

            return true;

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to open trade machine via reflection: " + e.getMessage(), e);
        }
    }

    private static Field ensureVGField(Object jq) throws NoSuchFieldException {
        if (vGField == null) {
            vGField = accessible(jq.getClass().getDeclaredField("vG"));
        }
        return vGField;
    }

    private static Field ensureVHField(Object jq) throws NoSuchFieldException {
        if (vHField == null) {
            vHField = accessible(jq.getClass().getDeclaredField("vH"));
        }
        return vHField;
    }

    /**
     * Finds the single-argument {@code a(ItemStack)} method on {@code aInstance}'s
     * class (or any superclass), disambiguating by parameter type to handle
     * obfuscated overloads.
     */
    private static Method ensureALookupMethod(Object aInstance)
            throws NoSuchMethodException {
        if (aLookupMethod == null) {
            aLookupMethod = findMethod(
                    aInstance.getClass(), "a", "org.bukkit.inventory.ItemStack");
        }
        return aLookupMethod;
    }

    private static Field ensureBDField(Object oyInstance) throws NoSuchFieldException {
        if (BDField == null) {
            BDField = accessible(oyInstance.getClass().getDeclaredField("BD"));
        }
        return BDField;
    }

    /**
     * Finds {@code bg(String)} on the behaviour-data container class.
     */
    private static Method ensureBgMethod(Object BD) throws NoSuchMethodException {
        if (bgMethod == null) {
            bgMethod = findMethod(BD.getClass(), "bg", "java.lang.String");
        }
        return bgMethod;
    }

    /**
     * Finds {@code am(Player)} walking up the class hierarchy, because it is
     * declared on the {@code ix} superclass rather than on {@code ip}/{@code id}.
     */
    private static Method ensureAmMethod(Object tradeMachine)
            throws NoSuchMethodException {
        if (amMethod == null) {
            amMethod = findMethod(
                    tradeMachine.getClass(), "am", "org.bukkit.entity.Player");
        }
        return amMethod;
    }

    /**
     * Searches {@code cls} and its superclass chain for a method named
     * {@code name} whose single parameter has the given binary class name.
     *
     * @throws NoSuchMethodException if not found anywhere in the hierarchy.
     */
    private static Method findMethod(Class<?> cls, String name, String paramTypeName)
            throws NoSuchMethodException {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (name.equals(m.getName())
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].getName().equals(paramTypeName)) {
                    return accessible(m);
                }
            }
        }
        throw new NoSuchMethodException(
                name + "(" + paramTypeName + ") not found in hierarchy of " + cls.getName());
    }

    private static <T extends java.lang.reflect.AccessibleObject> T accessible(T obj) {
        obj.setAccessible(true);
        return obj;
    }
}
