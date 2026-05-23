package toutouchien.itemsadderadditions.feature.behaviour.builtin.connectable;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FacingDirectionTest {
    @Test
    void yaw0IsSouth() {
        assertEquals(FacingDirection.SOUTH, FacingDirection.fromYaw(0f));
    }

    @Test
    void yaw90IsWest() {
        assertEquals(FacingDirection.WEST, FacingDirection.fromYaw(90f));
    }

    @Test
    void yaw180IsNorth() {
        assertEquals(FacingDirection.NORTH, FacingDirection.fromYaw(180f));
    }

    @Test
    void yaw270IsEast() {
        assertEquals(FacingDirection.EAST, FacingDirection.fromYaw(270f));
    }

    @Test
    void yaw360WrapsSouth() {
        assertEquals(FacingDirection.SOUTH, FacingDirection.fromYaw(360f));
    }

    @Test
    void yawNegative180IsNorth() {
        assertEquals(FacingDirection.NORTH, FacingDirection.fromYaw(-180f));
    }

    @Test
    void yawNegative90IsEast() {
        assertEquals(FacingDirection.EAST, FacingDirection.fromYaw(-90f));
    }

    @Test
    void yaw44IsSouth() {
        assertEquals(FacingDirection.SOUTH, FacingDirection.fromYaw(44f));
    }

    @Test
    void yaw45IsWest() {
        assertEquals(FacingDirection.WEST, FacingDirection.fromYaw(45f));
    }

    @Test
    void yaw315IsSouth() {
        assertEquals(FacingDirection.SOUTH, FacingDirection.fromYaw(315f));
    }

    @Test
    void yaw314IsEast() {
        assertEquals(FacingDirection.EAST, FacingDirection.fromYaw(314f));
    }


    @Test
    void northOppositeIsSouth() {
        assertEquals(FacingDirection.SOUTH, FacingDirection.NORTH.opposite());
    }

    @Test
    void southOppositeIsNorth() {
        assertEquals(FacingDirection.NORTH, FacingDirection.SOUTH.opposite());
    }

    @Test
    void westOppositeIsEast() {
        assertEquals(FacingDirection.EAST, FacingDirection.WEST.opposite());
    }

    @Test
    void eastOppositeIsWest() {
        assertEquals(FacingDirection.WEST, FacingDirection.EAST.opposite());
    }

    @Test
    void doubleOppositeIsIdentity() {
        for (FacingDirection dir : FacingDirection.values()) {
            assertEquals(dir, dir.opposite().opposite());
        }
    }


    @Test
    void northCounterClockWiseIsWest() {
        assertEquals(FacingDirection.WEST, FacingDirection.NORTH.counterClockWise());
    }

    @Test
    void westCounterClockWiseIsSouth() {
        assertEquals(FacingDirection.SOUTH, FacingDirection.WEST.counterClockWise());
    }

    @Test
    void southCounterClockWiseIsEast() {
        assertEquals(FacingDirection.EAST, FacingDirection.SOUTH.counterClockWise());
    }

    @Test
    void eastCounterClockWiseIsNorth() {
        assertEquals(FacingDirection.NORTH, FacingDirection.EAST.counterClockWise());
    }

    @Test
    void fourCounterClockWisesReturnToStart() {
        for (FacingDirection dir : FacingDirection.values()) {
            FacingDirection rotated = dir
                    .counterClockWise().counterClockWise()
                    .counterClockWise().counterClockWise();
            assertEquals(dir, rotated);
        }
    }


    @Test
    void northClockWiseIsEast() {
        assertEquals(FacingDirection.EAST, FacingDirection.NORTH.clockWise());
    }

    @Test
    void clockWiseIsOppositeOfCounterClockWise() {
        for (FacingDirection dir : FacingDirection.values()) {
            assertEquals(dir.counterClockWise().opposite(), dir.clockWise());
        }
    }


    @Test
    void southYawIs0() {
        assertEquals(0f, FacingDirection.SOUTH.toYaw());
    }

    @Test
    void westYawIs90() {
        assertEquals(90f, FacingDirection.WEST.toYaw());
    }

    @Test
    void northYawIs180() {
        assertEquals(180f, FacingDirection.NORTH.toYaw());
    }

    @Test
    void eastYawIs270() {
        assertEquals(270f, FacingDirection.EAST.toYaw());
    }

    @Test
    void fromYawToYawRoundtrip() {
        for (FacingDirection dir : FacingDirection.values()) {
            assertEquals(dir, FacingDirection.fromYaw(dir.toYaw()));
        }
    }


    @Test
    void northAndEastArePerpendicular() {
        assertTrue(FacingDirection.NORTH.isPerpendicular(FacingDirection.EAST));
    }

    @Test
    void northAndWestArePerpendicular() {
        assertTrue(FacingDirection.NORTH.isPerpendicular(FacingDirection.WEST));
    }

    @Test
    void northAndSouthAreNotPerpendicular() {
        assertFalse(FacingDirection.NORTH.isPerpendicular(FacingDirection.SOUTH));
    }

    @Test
    void northAndNorthAreNotPerpendicular() {
        assertFalse(FacingDirection.NORTH.isPerpendicular(FacingDirection.NORTH));
    }

    @Test
    void eastAndSouthArePerpendicular() {
        assertTrue(FacingDirection.EAST.isPerpendicular(FacingDirection.SOUTH));
    }

    @Test
    void eastAndWestAreNotPerpendicular() {
        assertFalse(FacingDirection.EAST.isPerpendicular(FacingDirection.WEST));
    }


    @Test
    void northOffsetDecreasesZ() {
        Location origin = new Location(null, 0, 0, 0);
        Location result = FacingDirection.NORTH.offset(origin);
        assertEquals(0.0, result.getX());
        assertEquals(0.0, result.getY());
        assertEquals(-1.0, result.getZ());
    }

    @Test
    void southOffsetIncreasesZ() {
        Location origin = new Location(null, 0, 0, 0);
        Location result = FacingDirection.SOUTH.offset(origin);
        assertEquals(0.0, result.getX());
        assertEquals(0.0, result.getY());
        assertEquals(1.0, result.getZ());
    }

    @Test
    void westOffsetDecreasesX() {
        Location origin = new Location(null, 5, 64, 5);
        Location result = FacingDirection.WEST.offset(origin);
        assertEquals(4.0, result.getX());
        assertEquals(64.0, result.getY());
        assertEquals(5.0, result.getZ());
    }

    @Test
    void eastOffsetIncreasesX() {
        Location origin = new Location(null, 5, 64, 5);
        Location result = FacingDirection.EAST.offset(origin);
        assertEquals(6.0, result.getX());
        assertEquals(64.0, result.getY());
        assertEquals(5.0, result.getZ());
    }

    @Test
    void offsetDoesNotMutateOrigin() {
        Location origin = new Location(null, 3, 64, 3);
        FacingDirection.NORTH.offset(origin);
        // origin must remain at (3, 64, 3)
        assertEquals(3.0, origin.getX());
        assertEquals(3.0, origin.getZ());
    }

    @Test
    void offsetPreservesY() {
        Location origin = new Location(null, 0, 100, 0);
        for (FacingDirection dir : FacingDirection.values()) {
            assertEquals(100.0, dir.offset(origin).getY(), "Y should not change for " + dir);
        }
    }

    @Test
    void oppositeDirectionOffsetsCancelOut() {
        Location origin = new Location(null, 0, 64, 0);
        for (FacingDirection dir : FacingDirection.values()) {
            Location afterFirst = dir.offset(origin);
            Location afterSecond = dir.opposite().offset(afterFirst);
            assertEquals(origin.getBlockX(), afterSecond.getBlockX(), dir + " round-trip X");
            assertEquals(origin.getBlockZ(), afterSecond.getBlockZ(), dir + " round-trip Z");
        }
    }
}
