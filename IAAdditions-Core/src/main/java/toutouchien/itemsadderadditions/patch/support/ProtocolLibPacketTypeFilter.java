package toutouchien.itemsadderadditions.patch.support;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Reflection-only helpers for ItemsAdder bytecode patches that interact with
 * ProtocolLib without making ProtocolLib a compile-time dependency.
 */
@NullMarked
public final class ProtocolLibPacketTypeFilter {
    private ProtocolLibPacketTypeFilter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Removes ProtocolLib packet types that are not registered on the current
     * Minecraft server version.
     *
     * <p>The returned array keeps the runtime component type of {@code packetTypes}
     * (normally {@code com.comphenix.protocol.PacketType[]}) so patched call sites
     * can cast it back to their original signature.</p>
     *
     * @param packetTypes a ProtocolLib {@code PacketType[]} passed as {@code Object[]}
     * @return {@code packetTypes}, or a same-component-type array containing only supported entries
     */
    public static @Nullable Object[] supportedOnly(@Nullable Object[] packetTypes) {
        if (packetTypes == null || packetTypes.length == 0) {
            return packetTypes;
        }

        Object[] supported = Arrays.copyOf(packetTypes, packetTypes.length);
        int supportedCount = 0;
        for (Object packetType : packetTypes) {
            if (isSupported(packetType)) {
                supported[supportedCount++] = packetType;
            }
        }

        if (supportedCount == packetTypes.length) {
            return packetTypes;
        }

        return Arrays.copyOf(supported, supportedCount);
    }

    private static boolean isSupported(Object packetType) {
        try {
            Method isSupported = packetType.getClass().getMethod("isSupported");
            return Boolean.TRUE.equals(isSupported.invoke(packetType));
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Be conservative if ProtocolLib changes its API: preserve the
            // original packet type rather than accidentally disabling a listener.
            return true;
        }
    }
}
