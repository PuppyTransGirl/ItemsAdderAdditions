package toutouchien.itemsadderadditions.integration.bridge;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownBridge {
    private static final String[] ITEM_HASH_FIELD_NAMES = {"BR", "BH", "Bq"};

    // last known result per player/itemHash
    // true = allowed, false = on cooldown
    private static final Map<UUID, Map<Integer, Boolean>> STATE =
            new ConcurrentHashMap<>();
    private static final Map<Class<?>, Optional<Field>> ITEM_HASH_FIELDS =
            new ConcurrentHashMap<>();

    private CooldownBridge() {
    }

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

    /**
     * Called from injected bytecode inside lr.b() on IA builds whose obfuscated
     * item-hash field name varies between 4.0.17 releases.
     */
    public static boolean capture(boolean result, Object livingEntity, Object customItemData) {
        Integer itemHash;
        try {
            itemHash = itemHash(customItemData);
        } catch (RuntimeException ignored) {
            return result;
        }
        if (itemHash == null) return result;

        return capture(result, livingEntity, itemHash.intValue());
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

    private static Integer itemHash(Object customItemData) {
        if (customItemData == null) return null;

        Optional<Field> field = ITEM_HASH_FIELDS.computeIfAbsent(
                customItemData.getClass(),
                CooldownBridge::findItemHashField
        );
        if (field.isEmpty()) return null;

        try {
            return field.get().getInt(customItemData);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return null;
        }
    }

    private static Optional<Field> findItemHashField(Class<?> cls) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (String name : ITEM_HASH_FIELD_NAMES) {
                try {
                    Field field = c.getDeclaredField(name);
                    if (field.getType() != int.class) continue;
                    field.setAccessible(true);
                    return Optional.of(field);
                } catch (NoSuchFieldException | RuntimeException ignored) {
                }
            }
        }

        return Optional.empty();
    }
}
