package toutouchien.itemsadderadditions.utils.lava_fishing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LavaFishingSessionManager {
    private LavaFishingSessionManager() {
        throw new IllegalStateException("Utility class");
    }

    private static final Map<UUID, LavaBobberState> ACTIVE_SESSIONS = new HashMap<>();

    public static boolean hasSession(UUID uuid) {
        return ACTIVE_SESSIONS.containsKey(uuid);
    }

    public static void addSession(UUID uuid, LavaBobberState state) {
        ACTIVE_SESSIONS.put(uuid, state);
    }

    public static void removeSession(UUID uuid) {
        LavaBobberState state = ACTIVE_SESSIONS.remove(uuid);
        if (state != null && state.getHook() != null && state.getHook().isInLava())
            state.getHook().remove();
    }

    public static LavaBobberState getSession(UUID uuid) {
        return ACTIVE_SESSIONS.get(uuid);
    }
}
