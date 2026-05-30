package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import toutouchien.itemsadderadditions.common.utils.BlockCoord;

import static org.junit.jupiter.api.Assertions.*;

class OpenVariantStateTest {
    private static final BlockCoord KEY = new BlockCoord("world", 1, 2, 3);

    private ServerMock server;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void incrementStartsAtOneAndMarksOpen() {
        OpenVariantState state = new OpenVariantState();

        assertEquals(1, state.increment(KEY));
        assertTrue(state.isOpen(KEY));
    }

    @Test
    void incrementAccumulatesOpenCount() {
        OpenVariantState state = new OpenVariantState();
        state.increment(KEY);

        assertEquals(2, state.increment(KEY));
    }

    @Test
    void decrementReturnsRemainingCount() {
        OpenVariantState state = new OpenVariantState();
        state.increment(KEY);
        state.increment(KEY);

        assertEquals(1, state.decrement(KEY));
        assertTrue(state.isOpen(KEY));
    }

    @Test
    void decrementToZeroRemovesOpenCount() {
        OpenVariantState state = new OpenVariantState();
        state.increment(KEY);

        assertEquals(0, state.decrement(KEY));
        assertFalse(state.isOpen(KEY));
    }

    @Test
    void decrementMissingKeyReturnsNegativeAndKeepsClosed() {
        OpenVariantState state = new OpenVariantState();

        assertEquals(-1, state.decrement(KEY));
        assertFalse(state.isOpen(KEY));
    }

    @Test
    void liveEntityCanBeStoredReadAndRemoved() {
        OpenVariantState state = new OpenVariantState();
        Entity entity = server.addPlayer();

        state.liveEntity(KEY, entity);

        assertSame(entity, state.liveEntity(KEY));
        assertSame(entity, state.removeLiveEntity(KEY));
        assertNull(state.liveEntity(KEY));
    }

    @Test
    void removeMissingLiveEntityReturnsNull() {
        assertNull(new OpenVariantState().removeLiveEntity(KEY));
    }

    @Test
    void savedBlockIdCanBeRemovedOnce() {
        OpenVariantState state = new OpenVariantState();
        state.savedBlockId(KEY, "ns:block_north");

        assertEquals("ns:block_north", state.removeSavedBlockId(KEY));
        assertNull(state.removeSavedBlockId(KEY));
    }

    @Test
    void savedYawCanBeRemovedOnce() {
        OpenVariantState state = new OpenVariantState();
        state.savedYaw(KEY, 45f);

        assertEquals(45f, state.removeSavedYaw(KEY));
        assertNull(state.removeSavedYaw(KEY));
    }

    @Test
    void savedItemCanBeRemovedOnce() {
        OpenVariantState state = new OpenVariantState();
        ItemStack item = ItemStack.of(Material.DIAMOND);
        state.savedItem(KEY, item);

        assertSame(item, state.removeSavedItem(KEY));
        assertNull(state.removeSavedItem(KEY));
    }

    @Test
    void forgetRemovesOpenCountAndSavedStateButNotLiveEntity() {
        OpenVariantState state = new OpenVariantState();
        Entity entity = server.addPlayer();
        state.increment(KEY);
        state.liveEntity(KEY, entity);
        state.savedBlockId(KEY, "ns:block");
        state.savedYaw(KEY, 90f);
        state.savedItem(KEY, ItemStack.of(Material.STONE));

        state.forget(KEY);

        assertFalse(state.isOpen(KEY));
        assertNull(state.removeSavedBlockId(KEY));
        assertNull(state.removeSavedYaw(KEY));
        assertNull(state.removeSavedItem(KEY));
        assertSame(entity, state.liveEntity(KEY));
    }

    @Test
    void clearRemovesAllStoredState() {
        OpenVariantState state = new OpenVariantState();
        state.increment(KEY);
        state.savedBlockId(KEY, "ns:block");
        state.savedYaw(KEY, 90f);
        state.savedItem(KEY, ItemStack.of(Material.STONE));

        state.clear();

        assertFalse(state.isOpen(KEY));
        assertNull(state.removeSavedBlockId(KEY));
        assertNull(state.removeSavedYaw(KEY));
        assertNull(state.removeSavedItem(KEY));
    }
}
