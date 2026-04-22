package toutouchien.itemsadderadditions.bridge;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge between the ASM patches (which run in NMS classloader space) and the
 * plugin's behaviour layer.
 *
 * <p>Instead of trying to reverse-look-up a furniture entity from the bed
 * location stored by Minecraft (which is just AIR for custom beds), we keep an
 * explicit registry of UUIDs that are currently sleeping in a custom bed.
 * {@link #checkBedExists} returns {@code true} for any registered UUID so the
 * vanilla "kick the player out if the bed is gone" tick never fires.
 */
public final class BedBridge {

    private static final String LOG_TAG = "BedBridge";

    /**
     * UUIDs of players currently sleeping in a custom (non-vanilla) bed.
     */
    private static final Set<UUID> CUSTOM_SLEEPERS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Map<UUID, Float> SLEEPER_YAWS = new ConcurrentHashMap<>();

    private BedBridge() {
    }

    /**
     * Call this right before {@link Player#sleep} for a custom bed.
     */
    public static void registerCustomSleeper(UUID uuid, float yaw) {
        CUSTOM_SLEEPERS.add(uuid);
        SLEEPER_YAWS.put(uuid, yaw);
        Log.debug(LOG_TAG, "Registered custom sleeper: {}", uuid);
        Log.debug(LOG_TAG, "Registered custom sleeper: {} yaw={}", uuid, yaw);
    }

    /**
     * Call this on wakeup, quit, or furniture break.
     * Safe to call even if the UUID was never registered.
     */
    public static void unregisterCustomSleeper(UUID uuid) {
        boolean removed = CUSTOM_SLEEPERS.remove(uuid);
        if (removed)
            Log.debug(LOG_TAG, "Unregistered custom sleeper: {}", uuid);
    }

    /**
     * Returns {@code Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW)} via
     * reflection so the GETSTATIC never lives in raw bytecode (which crashes when
     * the field name differs between mappings).
     *
     * <p>If resolution fails, logs the available enum constant names and returns
     * {@code null}.  The patch treats {@code null} as "fall through to vanilla".</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    public static Object sleepNotPossibleNow() {
        try {
            Class<? extends Enum> problemClass = (Class<? extends Enum>)
                    Class.forName("net.minecraft.world.entity.player.Player$BedSleepingProblem");

            Enum<?>[] constants = (Enum<?>[]) problemClass.getEnumConstants();

            // Try the Mojang-mapped name first.
            for (Enum<?> c : constants) {
                if (c.name().equals("NOT_POSSIBLE_NOW")) {
                    return eitherLeft(c);
                }
            }

            // Name not found - dump every available constant so the developer knows
            // exactly what name to use on this build.
            StringBuilder sb = new StringBuilder(
                    "BedBridge.sleepNotPossibleNow: 'NOT_POSSIBLE_NOW' not found in " +
                            "Player$BedSleepingProblem. Available constants: "
            );
            for (Enum<?> c : constants) sb.append(c.name()).append(", ");
            Log.error(LOG_TAG, sb.toString().stripTrailing().replaceAll(",$", ""));
            return null;

        } catch (Exception e) {
            Log.error(LOG_TAG, "sleepNotPossibleNow: reflection failed - {}: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Calls {@code Either.left(value)} reflectively.
     */
    @Nullable
    private static Object eitherLeft(Object value) {
        try {
            Class<?> eitherClass = Class.forName("com.mojang.datafixers.util.Either");
            return eitherClass.getMethod("left", Object.class).invoke(null, value);
        } catch (Exception e) {
            Log.error(LOG_TAG, "eitherLeft: reflection failed - {}: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    public static float getSleeperYaw(UUID uuid) {
        return SLEEPER_YAWS.getOrDefault(uuid, 0F);
    }


    public static boolean checkBedExists(Entity entity) {
        if (!(entity instanceof Player player)) {
            // Non-players: don't interfere with vanilla logic.
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (CUSTOM_SLEEPERS.contains(uuid)) {
            Log.debug(LOG_TAG, "checkBedExists: {} is a custom sleeper → true", player.getName());
            return true;
        }

        // Not one of ours - the patch always injects a return so we must
        // return something for vanilla beds too. true is safe here because
        // vanilla beds pass their own checkBedExists independently of this
        // patch (the patch only short-circuits when we return early).
        Log.debug(LOG_TAG, "checkBedExists: {} not a custom sleeper → true (vanilla passthrough)", player.getName());
        return true;
    }
}
