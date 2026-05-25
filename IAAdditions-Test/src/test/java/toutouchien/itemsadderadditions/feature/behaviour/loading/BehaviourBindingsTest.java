package toutouchien.itemsadderadditions.feature.behaviour.loading;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourExecutor;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourHost;
import toutouchien.itemsadderadditions.feature.behaviour.annotation.Behaviour;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BehaviourBindingsTest {
    @AfterEach
    void clearBindings() {
        BehaviourBindings.clear();
    }

    @Test
    void getMissingId_returnsEmptyList() {
        assertTrue(BehaviourBindings.get("ns:missing").isEmpty());
    }

    @Test
    void hasMissingId_returnsFalse() {
        assertFalse(BehaviourBindings.has("ns:missing"));
    }

    @Test
    void add_thenGetReturnsExecutor() {
        DummyBehaviour executor = new DummyBehaviour();
        BehaviourBindings.add("ns:block", executor);

        assertEquals(List.of(executor), BehaviourBindings.get("ns:block"));
    }

    @Test
    void hasBoundId_returnsTrue() {
        BehaviourBindings.add("ns:block", new DummyBehaviour());

        assertTrue(BehaviourBindings.has("ns:block"));
    }

    @Test
    void getReturnsDefensiveCopy() {
        DummyBehaviour executor = new DummyBehaviour();
        BehaviourBindings.add("ns:block", executor);

        List<BehaviourExecutor> result = BehaviourBindings.get("ns:block");

        assertThrows(UnsupportedOperationException.class, () -> result.add(new DummyBehaviour()));
        assertEquals(List.of(executor), BehaviourBindings.get("ns:block"));
    }

    @Test
    void getFallsBackToBaseIdForNorthRotation() {
        DummyBehaviour executor = new DummyBehaviour();
        BehaviourBindings.add("ns:block", executor);

        assertEquals(List.of(executor), BehaviourBindings.get("ns:block_north"));
    }

    @Test
    void getFallsBackToBaseIdForDownRotation() {
        DummyBehaviour executor = new DummyBehaviour();
        BehaviourBindings.add("ns:block", executor);

        assertEquals(List.of(executor), BehaviourBindings.get("ns:block_down"));
    }

    @Test
    void getPrefersExactRotatedBindingOverBaseBinding() {
        DummyBehaviour base = new DummyBehaviour();
        DummyBehaviour rotated = new DummyBehaviour();
        BehaviourBindings.add("ns:block", base);
        BehaviourBindings.add("ns:block_west", rotated);

        assertEquals(List.of(rotated), BehaviourBindings.get("ns:block_west"));
    }

    @Test
    void clearUnloadsLoadedExecutors() {
        DummyBehaviour first = new DummyBehaviour();
        DummyBehaviour second = new DummyBehaviour();
        BehaviourHost host = new BehaviourHost("ns:block", ItemCategory.BLOCK, null);
        first.load(host);
        second.load(host);
        BehaviourBindings.add("ns:block", first);
        BehaviourBindings.add("ns:other", second);

        BehaviourBindings.clear();

        assertEquals(1, first.unloadCalls);
        assertEquals(1, second.unloadCalls);
        assertNull(first.host());
        assertNull(second.host());
        assertFalse(BehaviourBindings.has("ns:block"));
    }

    @Test
    void clearDoesNotCallUnloadOnNeverLoadedExecutor() {
        DummyBehaviour executor = new DummyBehaviour();
        BehaviourBindings.add("ns:block", executor);

        BehaviourBindings.clear();

        assertEquals(0, executor.unloadCalls);
    }

    @Behaviour(key = "dummy")
    private static final class DummyBehaviour extends BehaviourExecutor {
        private int unloadCalls;

        @Override
        protected void onLoad(BehaviourHost host) {
        }

        @Override
        protected void onUnload(BehaviourHost host) {
            unloadCalls++;
        }
    }
}
