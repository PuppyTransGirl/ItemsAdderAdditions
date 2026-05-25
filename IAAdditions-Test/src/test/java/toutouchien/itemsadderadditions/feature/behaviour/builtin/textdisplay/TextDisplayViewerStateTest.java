package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayHandle;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TextDisplayViewerStateTest {
    private static TextDisplayDisplayKey key(String displayId) {
        return new TextDisplayDisplayKey(UUID.randomUUID(), displayId);
    }

    private static TextDisplayVisual visual() {
        return new TextDisplayVisual(new PacketTextDisplayHandle(1, UUID.randomUUID()));
    }

    @Test
    void initial_isEmpty() {
        assertTrue(new TextDisplayViewerState().isEmpty());
    }

    @Test
    void get_missingKey_returnsNull() {
        assertNull(new TextDisplayViewerState().get(key("d1")));
    }

    @Test
    void put_thenGet_returnsVisual() {
        var state = new TextDisplayViewerState();
        var k = key("d1");
        var v = visual();
        state.put(k, v);
        assertSame(v, state.get(k));
    }

    @Test
    void put_addsToVisibleDisplays() {
        var state = new TextDisplayViewerState();
        var k = key("d1");
        state.put(k, visual());
        assertEquals(1, state.visibleDisplays().size());
        assertTrue(state.visibleDisplays().containsKey(k));
    }

    @Test
    void put_clearsSpawnAttemptForKey() {
        var state = new TextDisplayViewerState();
        var k = key("d1");
        state.markSpawnFailed(k, 10L);
        assertFalse(state.canAttemptSpawn(k, 10L));
        state.put(k, visual());
        assertTrue(state.canAttemptSpawn(k, 10L));
    }

    @Test
    void isEmpty_afterPut_returnsFalse() {
        var state = new TextDisplayViewerState();
        state.put(key("d1"), visual());
        assertFalse(state.isEmpty());
    }

    @Test
    void canAttemptSpawn_newKey_alwaysTrue() {
        var state = new TextDisplayViewerState();
        assertTrue(state.canAttemptSpawn(key("d1"), 0L));
    }

    @Test
    void markSpawnFailed_setsNextAttemptInFuture() {
        var state = new TextDisplayViewerState();
        var k = key("d1");
        state.markSpawnFailed(k, 10L);
        assertFalse(state.canAttemptSpawn(k, 10L));
        assertFalse(state.canAttemptSpawn(k, 20L));
        assertTrue(state.canAttemptSpawn(k, 31L));
    }

    @Test
    void shouldLogUpdateFailure_firstCall_returnsTrue() {
        var state = new TextDisplayViewerState();
        var k = key("d1");
        assertTrue(state.shouldLogUpdateFailure(k, 0L));
    }

    @Test
    void shouldLogUpdateFailure_withinCooldown_returnsFalse() {
        var state = new TextDisplayViewerState();
        var k = key("d1");
        state.shouldLogUpdateFailure(k, 0L);
        assertFalse(state.shouldLogUpdateFailure(k, 5L));
    }

    @Test
    void shouldLogUpdateFailure_afterCooldown_returnsTrue() {
        var state = new TextDisplayViewerState();
        var k = key("d1");
        state.shouldLogUpdateFailure(k, 0L);
        assertTrue(state.shouldLogUpdateFailure(k, 21L));
    }

    @Test
    void clearUpdateFailure_allowsImmediateReLog() {
        var state = new TextDisplayViewerState();
        var k = key("d1");
        state.shouldLogUpdateFailure(k, 0L);
        state.clearUpdateFailure(k);
        assertTrue(state.shouldLogUpdateFailure(k, 0L));
    }

    @Test
    void retainSpawnAttempts_removesAbsentKeys() {
        var state = new TextDisplayViewerState();
        var k1 = key("d1");
        var k2 = key("d2");
        state.markSpawnFailed(k1, 0L);
        state.markSpawnFailed(k2, 0L);
        state.retainSpawnAttempts(Set.of(k1));
        assertFalse(state.canAttemptSpawn(k1, 0L));
        assertTrue(state.canAttemptSpawn(k2, 0L));
    }

    @Test
    void retainSpawnAttempts_removesFailureLogsForAbsentKeys() {
        var state = new TextDisplayViewerState();
        var k1 = key("d1");
        var k2 = key("d2");
        state.shouldLogUpdateFailure(k1, 0L);
        state.shouldLogUpdateFailure(k2, 0L);
        state.retainSpawnAttempts(Set.of(k1));
        assertTrue(state.shouldLogUpdateFailure(k2, 0L));
    }

    @Test
    void removeOwnerState_removesKeysForThatOwner() {
        var ownerId = UUID.randomUUID();
        var k = new TextDisplayDisplayKey(ownerId, "d1");
        var state = new TextDisplayViewerState();
        state.markSpawnFailed(k, 0L);
        state.shouldLogUpdateFailure(k, 0L);
        state.removeOwnerState(ownerId);
        assertTrue(state.canAttemptSpawn(k, 0L));
        assertTrue(state.shouldLogUpdateFailure(k, 0L));
    }

    @Test
    void removeOwnerState_keepsOtherOwners() {
        var owner1 = UUID.randomUUID();
        var owner2 = UUID.randomUUID();
        var k1 = new TextDisplayDisplayKey(owner1, "d1");
        var k2 = new TextDisplayDisplayKey(owner2, "d2");
        var state = new TextDisplayViewerState();
        state.markSpawnFailed(k1, 0L);
        state.markSpawnFailed(k2, 0L);
        state.removeOwnerState(owner1);
        assertTrue(state.canAttemptSpawn(k1, 0L));
        assertFalse(state.canAttemptSpawn(k2, 0L));
    }

    @Test
    void clear_emptiesEverything() {
        var state = new TextDisplayViewerState();
        var k = key("d1");
        state.put(k, visual());
        state.markSpawnFailed(k, 0L);
        state.shouldLogUpdateFailure(k, 0L);
        state.clear();
        assertTrue(state.isEmpty());
        assertNull(state.get(k));
        assertTrue(state.canAttemptSpawn(k, 0L));
    }
}
