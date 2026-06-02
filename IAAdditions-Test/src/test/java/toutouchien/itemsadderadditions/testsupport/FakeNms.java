package toutouchien.itemsadderadditions.testsupport;

import toutouchien.itemsadderadditions.nms.api.INmsHandler;
import toutouchien.itemsadderadditions.nms.api.NmsManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Installs a deep-stub {@link INmsHandler} into the {@link NmsManager} singleton via
 * reflection, so production code paths that call {@code NmsManager.instance().handler()}
 * can run under MockBukkit without a real NMS module. Returned handler is a Mockito mock,
 * so calls (e.g. {@code advancements().award(...)}) can be verified.
 */
public final class FakeNms {
    private FakeNms() {
    }

    public static INmsHandler install() {
        INmsHandler handler = mock(INmsHandler.class, RETURNS_DEEP_STUBS);
        try {
            NmsManager.shutdown();
            Constructor<NmsManager> ctor = NmsManager.class.getDeclaredConstructor(INmsHandler.class);
            ctor.setAccessible(true);
            NmsManager manager = ctor.newInstance(handler);

            Field instance = NmsManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, manager);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to install fake NmsManager", e);
        }
        return handler;
    }

    public static void uninstall() {
        NmsManager.shutdown();
    }
}
