package toutouchien.itemsadderadditions.feature.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TriggerKeyTest {
    @Test
    void ofWithoutArgumentHasNullArgument() {
        TriggerKey key = TriggerKey.of(TriggerType.ITEM_INTERACT);
        assertNull(key.argument());
    }

    @Test
    void ofWithoutArgumentHasNoArgument() {
        TriggerKey key = TriggerKey.of(TriggerType.ITEM_INTERACT);
        assertFalse(key.hasArgument());
    }

    @Test
    void ofWithArgumentHasArgument() {
        TriggerKey key = TriggerKey.of(TriggerType.ITEM_INTERACT, "right");
        assertTrue(key.hasArgument());
        assertEquals("right", key.argument());
    }

    @Test
    void ofWithNullArgumentHasNoArgument() {
        TriggerKey key = TriggerKey.of(TriggerType.ITEM_INTERACT, null);
        assertFalse(key.hasArgument());
    }

    @Test
    void equalKeysAreEqual() {
        TriggerKey k1 = TriggerKey.of(TriggerType.ITEM_INTERACT, "right");
        TriggerKey k2 = TriggerKey.of(TriggerType.ITEM_INTERACT, "right");
        assertEquals(k1, k2);
    }

    @Test
    void differentArgumentsMakeKeysUnequal() {
        TriggerKey k1 = TriggerKey.of(TriggerType.ITEM_INTERACT, "right");
        TriggerKey k2 = TriggerKey.of(TriggerType.ITEM_INTERACT, "left");
        assertNotEquals(k1, k2);
    }

    @Test
    void differentTypeMakesKeysUnequal() {
        TriggerKey k1 = TriggerKey.of(TriggerType.ITEM_INTERACT, "right");
        TriggerKey k2 = TriggerKey.of(TriggerType.BLOCK_INTERACT, "right");
        assertNotEquals(k1, k2);
    }

    @Test
    void equalKeysHaveSameHashCode() {
        TriggerKey k1 = TriggerKey.of(TriggerType.FURNITURE_INTERACT, "left_shift");
        TriggerKey k2 = TriggerKey.of(TriggerType.FURNITURE_INTERACT, "left_shift");
        assertEquals(k1.hashCode(), k2.hashCode());
    }

    @Test
    void typeIsPreserved() {
        TriggerKey key = TriggerKey.of(TriggerType.ITEM_KILL);
        assertEquals(TriggerType.ITEM_KILL, key.type());
    }
}
