package toutouchien.itemsadderadditions.feature.behaviour;

import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.common.item.ItemCategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BehaviourHostTest {
    @Test
    void constructorRejectsBareId() {
        assertThrows(IllegalArgumentException.class,
                () -> new BehaviourHost("stone", ItemCategory.BLOCK, null));
    }

    @Test
    void namespaceReturnsPartBeforeColon() {
        BehaviourHost host = new BehaviourHost("custom:chair", ItemCategory.FURNITURE, null);

        assertEquals("custom", host.namespace());
    }

    @Test
    void idReturnsPartAfterColon() {
        BehaviourHost host = new BehaviourHost("custom:chair", ItemCategory.FURNITURE, null);

        assertEquals("chair", host.id());
    }

    @Test
    void storesCategory() {
        BehaviourHost host = new BehaviourHost("custom:chair", ItemCategory.COMPLEX_FURNITURE, null);

        assertEquals(ItemCategory.COMPLEX_FURNITURE, host.category());
    }
}
