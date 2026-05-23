package toutouchien.itemsadderadditions.feature.behaviour.builtin.connectable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacementSpecTest {
    @Test
    void storesVariantId() {
        assertEquals("ns:my_variant", new PlacementSpec("ns:my_variant", 90f).variantID());
    }

    @Test
    void storesYaw() {
        assertEquals(180f, new PlacementSpec("ns:x", 180f).yaw());
    }

    @Test
    void nullVariantIdAllowed() {
        assertNull(new PlacementSpec(null, 0f).variantID());
    }

    @Test
    void zeroYaw() {
        assertEquals(0f, new PlacementSpec("ns:x", 0f).yaw());
    }

    @Test
    void negativeYaw() {
        assertEquals(-90f, new PlacementSpec("ns:x", -90f).yaw());
    }

    @Test
    void equalSpecsAreEqual() {
        assertEquals(new PlacementSpec("ns:x", 90f), new PlacementSpec("ns:x", 90f));
    }

    @Test
    void differentYawNotEqual() {
        assertNotEquals(new PlacementSpec("ns:x", 90f), new PlacementSpec("ns:x", 180f));
    }

    @Test
    void differentVariantNotEqual() {
        assertNotEquals(new PlacementSpec("ns:x", 90f), new PlacementSpec("ns:y", 90f));
    }

    @Test
    void nullVsNonNullVariantNotEqual() {
        assertNotEquals(new PlacementSpec(null, 0f), new PlacementSpec("ns:x", 0f));
    }

    @Test
    void bothNullVariantEqual() {
        assertEquals(new PlacementSpec(null, 90f), new PlacementSpec(null, 90f));
    }
}
