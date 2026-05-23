package toutouchien.itemsadderadditions.patch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionConstraintTest {
    private static final Version V = Version.of("1.21.0", "4.0.15");

    @Test
    void alwaysReturnsTrue() {
        assertTrue(VersionConstraint.always().test(V));
    }

    @Test
    void neverReturnsFalse() {
        assertFalse(VersionConstraint.never().test(V));
    }

    @Test
    void negateAlwaysIsFalse() {
        assertFalse(VersionConstraint.always().negate().test(V));
    }

    @Test
    void negateNeverIsTrue() {
        assertTrue(VersionConstraint.never().negate().test(V));
    }

    @Test
    void doubleNegateAlwaysIsTrue() {
        assertTrue(VersionConstraint.always().negate().negate().test(V));
    }

    @Test
    void andBothTrueIsTrue() {
        assertTrue(VersionConstraint.always().and(VersionConstraint.always()).test(V));
    }

    @Test
    void andFirstFalseIsFalse() {
        assertFalse(VersionConstraint.never().and(VersionConstraint.always()).test(V));
    }

    @Test
    void andSecondFalseIsFalse() {
        assertFalse(VersionConstraint.always().and(VersionConstraint.never()).test(V));
    }

    @Test
    void andBothFalseIsFalse() {
        assertFalse(VersionConstraint.never().and(VersionConstraint.never()).test(V));
    }

    @Test
    void orBothTrueIsTrue() {
        assertTrue(VersionConstraint.always().or(VersionConstraint.always()).test(V));
    }

    @Test
    void orFirstTrueIsTrue() {
        assertTrue(VersionConstraint.always().or(VersionConstraint.never()).test(V));
    }

    @Test
    void orSecondTrueIsTrue() {
        assertTrue(VersionConstraint.never().or(VersionConstraint.always()).test(V));
    }

    @Test
    void orBothFalseIsFalse() {
        assertFalse(VersionConstraint.never().or(VersionConstraint.never()).test(V));
    }

    @Test
    void andWithConcreteRangeFilters() {
        VersionConstraint inRange = VersionRange.mc("1.20.0", "1.22.0");
        VersionConstraint outOfRange = VersionRange.mc("1.18.0", "1.19.9");

        assertTrue(VersionConstraint.always().and(inRange).test(V));
        assertFalse(VersionConstraint.always().and(outOfRange).test(V));
    }

    @Test
    void orWithConcreteRangeSelectsEither() {
        VersionConstraint inRange = VersionRange.mc("1.20.0", "1.22.0");
        VersionConstraint outOfRange = VersionRange.mc("1.18.0", "1.19.9");

        assertTrue(inRange.or(outOfRange).test(V));
        assertFalse(outOfRange.or(outOfRange).test(V));
    }

    @Test
    void composedChain() {
        // always AND (never OR always) == true
        VersionConstraint c = VersionConstraint.always()
                .and(VersionConstraint.never().or(VersionConstraint.always()));
        assertTrue(c.test(V));
    }

    @Test
    void negatedAndBecomesOr() {
        // De Morgan: NOT(A AND B) == NOT A OR NOT B
        // NOT(always AND always) == false; NOT always OR NOT always == false OR false == false
        VersionConstraint notAndAlways = VersionConstraint.always().and(VersionConstraint.always()).negate();
        VersionConstraint orNegations = VersionConstraint.always().negate().or(VersionConstraint.always().negate());
        // Both should agree on V
        assertFalse(notAndAlways.test(V));
        assertFalse(orNegations.test(V));
    }
}
