package toutouchien.itemsadderadditions.patch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionSetTest {
    @Test
    void mcSetMatchesContainedVersion() {
        VersionSet set = VersionSet.mc("1.20.4", "1.21.1");
        assertTrue(set.test(Version.of("1.20.4", "4.0.15")));
    }

    @Test
    void mcSetDoesNotMatchAbsentVersion() {
        VersionSet set = VersionSet.mc("1.20.4", "1.21.1");
        assertFalse(set.test(Version.of("1.19.0", "4.0.15")));
    }

    @Test
    void mcSetIgnoresIaAxis() {
        VersionSet set = VersionSet.mc("1.21.0");
        assertTrue(set.test(Version.of("1.21.0", "4.0.15")));
        assertTrue(set.test(Version.of("1.21.0", "99.99.99")));
    }

    @Test
    void mcSetNormalizesPreRelease() {
        VersionSet set = VersionSet.mc("1.21.0");
        // "1.21.0-beta-1" normalises to "1.21.0" and should match
        assertTrue(set.test(Version.of("1.21.0-beta-1", "4.0.15")));
    }


    @Test
    void iaSetMatchesContainedVersion() {
        VersionSet set = VersionSet.ia("4.0.15", "4.0.16");
        assertTrue(set.test(Version.of("1.21.0", "4.0.15")));
    }

    @Test
    void iaSetDoesNotMatchAbsentVersion() {
        VersionSet set = VersionSet.ia("4.0.15");
        assertFalse(set.test(Version.of("1.21.0", "4.0.16")));
    }

    @Test
    void iaSetIgnoresMcAxis() {
        VersionSet set = VersionSet.ia("4.0.15");
        assertTrue(set.test(Version.of("99.0.0", "4.0.15")));
    }


    @Test
    void pairSetMatchesExactPair() {
        VersionSet set = VersionSet.of(Version.of("1.21.0", "4.0.15"));
        assertTrue(set.test(Version.of("1.21.0", "4.0.15")));
    }

    @Test
    void pairSetDoesNotMatchWrongIa() {
        VersionSet set = VersionSet.of(Version.of("1.21.0", "4.0.15"));
        assertFalse(set.test(Version.of("1.21.0", "4.0.16")));
    }

    @Test
    void pairSetDoesNotMatchWrongMc() {
        VersionSet set = VersionSet.of(Version.of("1.21.0", "4.0.15"));
        assertFalse(set.test(Version.of("1.20.0", "4.0.15")));
    }

    @Test
    void pairSetNormalizesBetaInInput() {
        VersionSet set = VersionSet.of(Version.of("1.21.0", "4.0.15"));
        assertTrue(set.test(Version.of("1.21.0-beta-1", "4.0.15-alpha-2")));
    }

    @Test
    void pairSetMatchesMultiplePairs() {
        VersionSet set = VersionSet.of(
                Version.of("1.20.0", "4.0.15"),
                Version.of("1.21.0", "4.0.16")
        );
        assertTrue(set.test(Version.of("1.20.0", "4.0.15")));
        assertTrue(set.test(Version.of("1.21.0", "4.0.16")));
        assertFalse(set.test(Version.of("1.20.0", "4.0.16")));
    }
}
