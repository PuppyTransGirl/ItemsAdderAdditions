package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@NullMarked
public final class TextDisplayViewerState {
    private static final long FAILURE_LOG_COOLDOWN_TICKS = 20L;

    private final Map<TextDisplayDisplayKey, TextDisplayVisual> visibleDisplays = new HashMap<>();
    private final Map<TextDisplayDisplayKey, Long> nextSpawnAttempts = new HashMap<>();
    private final Map<TextDisplayDisplayKey, Long> nextUpdateFailureLogs = new HashMap<>();

    @Nullable
    public TextDisplayVisual get(TextDisplayDisplayKey key) {
        return visibleDisplays.get(key);
    }

    public void put(TextDisplayDisplayKey key, TextDisplayVisual visual) {
        visibleDisplays.put(key, visual);
        nextSpawnAttempts.remove(key);
        nextUpdateFailureLogs.remove(key);
    }

    public Map<TextDisplayDisplayKey, TextDisplayVisual> visibleDisplays() {
        return visibleDisplays;
    }

    public boolean canAttemptSpawn(TextDisplayDisplayKey key, long currentTick) {
        return currentTick >= nextSpawnAttempts.getOrDefault(key, 0L);
    }

    public void markSpawnFailed(TextDisplayDisplayKey key, long currentTick) {
        nextSpawnAttempts.put(key, currentTick + FAILURE_LOG_COOLDOWN_TICKS);
    }

    public boolean shouldLogUpdateFailure(TextDisplayDisplayKey key, long currentTick) {
        if (currentTick < nextUpdateFailureLogs.getOrDefault(key, 0L)) return false;
        nextUpdateFailureLogs.put(key, currentTick + FAILURE_LOG_COOLDOWN_TICKS);
        return true;
    }

    public void clearUpdateFailure(TextDisplayDisplayKey key) {
        nextUpdateFailureLogs.remove(key);
    }

    public void retainSpawnAttempts(Set<TextDisplayDisplayKey> desiredKeys) {
        nextSpawnAttempts.keySet().retainAll(desiredKeys);
        nextUpdateFailureLogs.keySet().retainAll(desiredKeys);
    }

    public void removeOwnerState(UUID ownerId) {
        nextSpawnAttempts.keySet().removeIf(key -> key.ownerId().equals(ownerId));
        nextUpdateFailureLogs.keySet().removeIf(key -> key.ownerId().equals(ownerId));
    }

    public boolean isEmpty() {
        return visibleDisplays.isEmpty() && nextSpawnAttempts.isEmpty() && nextUpdateFailureLogs.isEmpty();
    }

    public void clear() {
        visibleDisplays.clear();
        nextSpawnAttempts.clear();
        nextUpdateFailureLogs.clear();
    }
}
