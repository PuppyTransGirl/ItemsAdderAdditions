package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

@NullMarked
final class StorageSessionRegistry {
    private static final double SAME_LOCATION_DISTANCE_SQUARED = 0.01;

    private final Map<UUID, StorageSession> sessions = new HashMap<>();

    private static boolean sameLocation(Location first, Location second) {
        return sameWorld(first, second) && first.distanceSquared(second) < SAME_LOCATION_DISTANCE_SQUARED;
    }

    private static boolean sameWorld(Location first, Location second) {
        return first.getWorld() != null && first.getWorld().equals(second.getWorld());
    }

    void add(StorageSession session) {
        sessions.put(session.player().getUniqueId(), session);
    }

    @Nullable
    StorageSession remove(UUID uuid) {
        return sessions.remove(uuid);
    }

    boolean hasAt(Location location) {
        return sessions.values().stream().anyMatch(session -> sameLocation(session.holderLocation(), location));
    }

    @Nullable
    Inventory liveInventoryAt(Location location) {
        for (StorageSession session : sessions.values()) {
            if (sameLocation(session.holderLocation(), location)) {
                return session.inventory();
            }
        }
        return null;
    }

    List<StorageSession> near(Location location, double maxDistanceSquared) {
        List<StorageSession> matches = new ArrayList<>();
        for (StorageSession session : sessions.values()) {
            Location holder = session.holderLocation();
            if (sameWorld(holder, location) && holder.distanceSquared(location) <= maxDistanceSquared) {
                matches.add(session);
            }
        }
        return matches;
    }

    Collection<StorageSession> all() {
        return List.copyOf(sessions.values());
    }

    void clear() {
        sessions.clear();
    }
}
