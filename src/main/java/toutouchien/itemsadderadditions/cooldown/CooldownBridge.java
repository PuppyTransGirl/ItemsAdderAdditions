package toutouchien.itemsadderadditions.cooldown;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownBridge {
    private CooldownBridge() {}

    // last known result per player/itemHash
    // true = allowed, false = on cooldown
    private static final Map<UUID, Map<Integer, Boolean>> STATE =
            new ConcurrentHashMap<>();

    /**
     * Called from injected bytecode inside lr.b().
     */
    public static boolean capture(boolean result, Object livingEntity, int itemHash) {
        try {
            LivingEntity le = (LivingEntity) livingEntity;
            STATE.computeIfAbsent(le.getUniqueId(), k -> new ConcurrentHashMap<>())
                 .put(itemHash, result);
        } catch (Exception ignored) {

        }

        return result; // always pass through the original value
    }

    public static boolean isOnCooldown(Player player, int itemHash) {
        Map<Integer, Boolean> entry = STATE.get(player.getUniqueId());
        if (entry == null) return false;

        Boolean result = entry.get(itemHash);
        if (result == null) return false;

        return !result; // result = false means blocked = true
    }

    public static void clear(UUID player) {
        STATE.remove(player);
    }
}
