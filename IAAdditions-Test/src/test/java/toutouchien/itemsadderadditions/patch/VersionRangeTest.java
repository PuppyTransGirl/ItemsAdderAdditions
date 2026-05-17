package toutouchien.itemsadderadditions.patch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionRangeTest {
    @Test
    void mcInRange() {
        VersionRange range = VersionRange.mc("1.20.0", "1.21.0");
        assertTrue(range.test(Version.of("1.20.4", "4.0.15")));
    }

    @Test
    void mcBelowMin() {
        VersionRange range = VersionRange.mc("1.20.0", "1.21.0");
        assertFalse(range.test(Version.of("1.19.0", "4.0.15")));
    }

    @Test
    void mcAboveMax() {
        VersionRange range = VersionRange.mc("1.20.0", "1.21.0");
        assertFalse(range.test(Version.of("1.22.0", "4.0.15")));
    }

    @Test
    void mcAtMinBound() {
        VersionRange range = VersionRange.mc("1.20.0", "1.21.0");
        assertTrue(range.test(Version.of("1.20.0", "4.0.15")));
    }

    @Test
    void mcAtMaxBound() {
        VersionRange range = VersionRange.mc("1.20.0", "1.21.0");
        assertTrue(range.test(Version.of("1.21.0", "4.0.15")));
    }

    @Test
    void mcUnboundedMinAcceptsAnything() {
        VersionRange range = VersionRange.mc(null, "1.21.0");
        assertTrue(range.test(Version.of("1.0.0", "4.0.15")));
    }

    @Test
    void mcUnboundedMaxAcceptsAnything() {
        VersionRange range = VersionRange.mc("1.20.0", null);
        assertTrue(range.test(Version.of("99.0.0", "4.0.15")));
    }


    @Test
    void iaInRange() {
        VersionRange range = VersionRange.ia("4.0.15", "4.0.17");
        assertTrue(range.test(Version.of("1.21.0", "4.0.16")));
    }

    @Test
    void iaBelowMin() {
        VersionRange range = VersionRange.ia("4.0.15", "4.0.17");
        assertFalse(range.test(Version.of("1.21.0", "4.0.14")));
    }

    @Test
    void iaAboveMax() {
        VersionRange range = VersionRange.ia("4.0.15", "4.0.17");
        assertFalse(range.test(Version.of("1.21.0", "4.0.18")));
    }

    @Test
    void iaAnyMcAccepted() {
        VersionRange range = VersionRange.ia("4.0.15", "4.0.17");
        assertTrue(range.test(Version.of("1.0.0", "4.0.15")));
        assertTrue(range.test(Version.of("99.0.0", "4.0.15")));
    }


    @Test
    void bothAxesMustMatch() {
        VersionRange range = VersionRange.of("1.20.0", "1.21.0", "4.0.15", "4.0.17");
        assertTrue(range.test(Version.of("1.20.4", "4.0.16")));
    }

    @Test
    void mcOutOfRangeButIaIn() {
        VersionRange range = VersionRange.of("1.20.0", "1.21.0", "4.0.15", "4.0.17");
        assertFalse(range.test(Version.of("1.22.0", "4.0.16")));
    }

    @Test
    void mcInRangeButIaOut() {
        VersionRange range = VersionRange.of("1.20.0", "1.21.0", "4.0.15", "4.0.17");
        assertFalse(range.test(Version.of("1.21.0", "4.0.18")));
    }
}
