package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextDisplayLocationMathTest {
    private static final double DELTA = 1e-6;


    @Test
    void rotateY_zeroAngle_vectorUnchanged() {
        Vector v = TextDisplayLocationMath.rotateY(new Vector(1, 0, 0), 0f);
        assertEquals(1.0, v.getX(), DELTA);
        assertEquals(0.0, v.getY(), DELTA);
        assertEquals(0.0, v.getZ(), DELTA);
    }

    @Test
    void rotateY_90degrees_xBecomesZ() {
        // cos(90°)=0, sin(90°)=1 -> x' = 1*0 - 0*1 = 0, z' = 1*1 + 0*0 = 1
        Vector v = TextDisplayLocationMath.rotateY(new Vector(1, 0, 0), 90f);
        assertEquals(0.0, v.getX(), DELTA);
        assertEquals(0.0, v.getY(), DELTA);
        assertEquals(1.0, v.getZ(), DELTA);
    }

    @Test
    void rotateY_180degrees_xFlips() {
        Vector v = TextDisplayLocationMath.rotateY(new Vector(1, 0, 0), 180f);
        assertEquals(-1.0, v.getX(), DELTA);
        assertEquals(0.0, v.getY(), DELTA);
        assertEquals(0.0, v.getZ(), DELTA);
    }

    @Test
    void rotateY_270degrees_xBecomesNegativeZ() {
        // cos(270°)=0, sin(270°)=-1 -> x' = 1*0 - 0*(-1) = 0, z' = 1*(-1) + 0*0 = -1
        Vector v = TextDisplayLocationMath.rotateY(new Vector(1, 0, 0), 270f);
        assertEquals(0.0, v.getX(), DELTA);
        assertEquals(0.0, v.getY(), DELTA);
        assertEquals(-1.0, v.getZ(), DELTA);
    }

    @Test
    void rotateY_preservesYComponent() {
        Vector v = TextDisplayLocationMath.rotateY(new Vector(3, 7, 0), 45f);
        assertEquals(7.0, v.getY(), DELTA);
    }

    @Test
    void rotateY_negativeAngle_reverseRotation() {
        // -90°: should be the reverse of +90°
        Vector pos = TextDisplayLocationMath.rotateY(new Vector(1, 0, 0), 90f);
        Vector neg = TextDisplayLocationMath.rotateY(pos, -90f);
        assertEquals(1.0, neg.getX(), DELTA);
        assertEquals(0.0, neg.getZ(), DELTA);
    }


    @Test
    void snapYaw_zero_staysZero() {
        assertEquals(0f, TextDisplayLocationMath.snapYaw(0f), 0.001f);
    }

    @Test
    void snapYaw_44_snapsTo0() {
        assertEquals(0f, TextDisplayLocationMath.snapYaw(44f), 0.001f);
    }

    @Test
    void snapYaw_45_snapsTo90() {
        // Math.round(0.5f) = 1 in Java -> 1 * 90 = 90
        assertEquals(90f, TextDisplayLocationMath.snapYaw(45f), 0.001f);
    }

    @Test
    void snapYaw_135_snapsTo180() {
        // Math.round(1.5f) = 2 -> 2 * 90 = 180
        assertEquals(180f, TextDisplayLocationMath.snapYaw(135f), 0.001f);
    }

    @Test
    void snapYaw_negative45_snapsTo0() {
        // Math.round(-0.5f) = 0 -> 0 * 90 = 0
        assertEquals(0f, TextDisplayLocationMath.snapYaw(-45f), 0.001f);
    }

    @Test
    void snapYaw_negative180_normalizesToPositive180() {
        // Math.round(-2f) = -2 -> -180, then -180 <= -180 so add 360 -> 180
        assertEquals(180f, TextDisplayLocationMath.snapYaw(-180f), 0.001f);
    }


    @Test
    void blockYaw_southSuffix_returns0() {
        assertEquals(0.0f, TextDisplayLocationMath.blockYaw("ns:block_south", 0f), 0.001f);
    }

    @Test
    void blockYaw_westSuffix_returns90() {
        assertEquals(90.0f, TextDisplayLocationMath.blockYaw("ns:block_west", 0f), 0.001f);
    }

    @Test
    void blockYaw_northSuffix_returns180() {
        assertEquals(180.0f, TextDisplayLocationMath.blockYaw("ns:block_north", 0f), 0.001f);
    }

    @Test
    void blockYaw_eastSuffix_returnsNegative90() {
        assertEquals(-90.0f, TextDisplayLocationMath.blockYaw("ns:block_east", 0f), 0.001f);
    }

    @Test
    void blockYaw_noSuffix_snapsPlayerYaw() {
        // Player yaw = 44f -> snapYaw(44f) = 0f
        assertEquals(0f, TextDisplayLocationMath.blockYaw("ns:block", 44f), 0.001f);
    }

    @Test
    void blockYaw_noSuffix_playerYaw90_snapsTo90() {
        assertEquals(90f, TextDisplayLocationMath.blockYaw("ns:chair", 90f), 0.001f);
    }


    @Test
    void applyLocalOffset_zeroYaw_addsDirectly() {
        Location base = new Location(null, 10, 64, 10);
        Vector offset = new Vector(1, 0, 0);

        Location result = TextDisplayLocationMath.applyLocalOffset(base, 0f, offset);

        assertEquals(11.0, result.getX(), DELTA);
        assertEquals(64.0, result.getY(), DELTA);
        assertEquals(10.0, result.getZ(), DELTA);
    }

    @Test
    void applyLocalOffset_90yaw_rotatesOffset() {
        Location base = new Location(null, 0, 0, 0);
        Vector offset = new Vector(1, 0, 0);

        Location result = TextDisplayLocationMath.applyLocalOffset(base, 90f, offset);

        // rotateY((1,0,0), 90°) = (0, 0, 1)
        assertEquals(0.0, result.getX(), DELTA);
        assertEquals(0.0, result.getY(), DELTA);
        assertEquals(1.0, result.getZ(), DELTA);
    }

    @Test
    void applyLocalOffset_preservesBaseLocation() {
        Location base = new Location(null, 5, 10, 15);
        Location original = base.clone();
        TextDisplayLocationMath.applyLocalOffset(base, 0f, new Vector(1, 2, 3));

        // base must not be mutated (applyLocalOffset clones it)
        assertEquals(original.getX(), base.getX(), DELTA);
        assertEquals(original.getY(), base.getY(), DELTA);
        assertEquals(original.getZ(), base.getZ(), DELTA);
    }

    @Test
    void applyLocalOffset_yComponentUnrotated() {
        Location base = new Location(null, 0, 0, 0);
        Vector offset = new Vector(0, 5, 0);

        Location result = TextDisplayLocationMath.applyLocalOffset(base, 45f, offset);

        assertEquals(5.0, result.getY(), DELTA);
    }
}
