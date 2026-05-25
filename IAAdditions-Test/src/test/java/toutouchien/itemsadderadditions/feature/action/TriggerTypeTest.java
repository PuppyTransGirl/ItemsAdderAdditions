package toutouchien.itemsadderadditions.feature.action;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class TriggerTypeTest {
    @Test
    void enumContainsExpectedTotalCount() {
        assertEquals(68, TriggerType.values().length);
    }

    @Test
    void blockTriggersArePresentInDeclarationOrder() {
        assertEquals(TriggerType.BLOCK_INTERACT, TriggerType.values()[0]);
        assertEquals(TriggerType.PLACED_BLOCK_BREAK, TriggerType.values()[1]);
    }

    @Test
    void itemTriggersContainInventoryLifecycleEvents() {
        EnumSet<TriggerType> values = EnumSet.allOf(TriggerType.class);

        assertTrue(values.containsAll(Arrays.asList(
                TriggerType.ITEM_DROP,
                TriggerType.ITEM_PICKUP,
                TriggerType.ITEM_HELD,
                TriggerType.ITEM_HELD_OFFHAND,
                TriggerType.ITEM_UNHELD,
                TriggerType.ITEM_UNHELD_OFFHAND,
                TriggerType.ITEM_BREAK
        )));
    }

    @Test
    void itemTriggersContainProjectileEvents() {
        EnumSet<TriggerType> values = EnumSet.allOf(TriggerType.class);

        assertTrue(values.containsAll(Arrays.asList(
                TriggerType.ITEM_BOW_SHOT,
                TriggerType.ITEM_THROW,
                TriggerType.ITEM_HIT_GROUND,
                TriggerType.ITEM_HIT_ENTITY
        )));
    }

    @Test
    void furnitureTriggersMirrorConsumableAndRangedEvents() {
        EnumSet<TriggerType> values = EnumSet.allOf(TriggerType.class);

        assertTrue(values.containsAll(Arrays.asList(
                TriggerType.FURNITURE_EAT,
                TriggerType.FURNITURE_DRINK,
                TriggerType.FURNITURE_BOW_SHOT,
                TriggerType.FURNITURE_GUN_SHOT,
                TriggerType.FURNITURE_GUN_NO_AMMO,
                TriggerType.FURNITURE_GUN_RELOAD
        )));
    }

    @Test
    void complexFurnitureHasDedicatedInteractTrigger() {
        assertNotNull(TriggerType.valueOf("COMPLEX_FURNITURE_INTERACT"));
    }

    @Test
    void valueOfRoundTripsEveryTriggerName() {
        for (TriggerType type : TriggerType.values()) {
            assertSame(type, TriggerType.valueOf(type.name()));
        }
    }
}
