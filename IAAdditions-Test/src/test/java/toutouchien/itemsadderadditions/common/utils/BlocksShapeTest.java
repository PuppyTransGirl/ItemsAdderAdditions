package toutouchien.itemsadderadditions.common.utils;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlocksShapeTest {
    private static Location center() {
        return new Location(null, 0, 0, 0);
    }

    @Test
    void coneIsDirectional() {
        assertTrue(BlocksShape.CONE.isDirectional());
    }

    @Test
    void beamIsDirectional() {
        assertTrue(BlocksShape.BEAM.isDirectional());
    }

    @Test
    void pyramidIsDirectional() {
        assertTrue(BlocksShape.PYRAMID.isDirectional());
    }

    @Test
    void cuboidNotDirectional() {
        assertFalse(BlocksShape.CUBOID.isDirectional());
    }

    @Test
    void rhombusNotDirectional() {
        assertFalse(BlocksShape.RHOMBUS.isDirectional());
    }

    @Test
    void sphereNotDirectional() {
        assertFalse(BlocksShape.SPHERE.isDirectional());
    }

    @Test
    void cylinderNotDirectional() {
        assertFalse(BlocksShape.CYLINDER.isDirectional());
    }

    @Test
    void shellNotDirectional() {
        assertFalse(BlocksShape.SHELL.isDirectional());
    }

    @Test
    void torusNotDirectional() {
        assertFalse(BlocksShape.TORUS.isDirectional());
    }

    @Test
    void cuboidZeroRadiusHasOneBlock() {
        assertEquals(1, BlocksShape.CUBOID.collect(center(), 0, 0, 0).size());
    }

    @Test
    void cuboidAsymmetricCount() {
        // (2*1+1)*(2*2+1)*(2*3+1) = 3*5*7 = 105
        assertEquals(105, BlocksShape.CUBOID.collect(center(), 1, 2, 3).size());
    }

    @Test
    void cuboidSymmetricCount() {
        // (2*2+1)^3 = 5^3 = 125
        assertEquals(125, BlocksShape.CUBOID.collect(center(), 2, 2, 2).size());
    }

    @Test
    void cuboidCenterBlockPresent() {
        Location c = new Location(null, 5, 10, 15);
        List<Location> result = BlocksShape.CUBOID.collect(c, 1, 1, 1);
        boolean found = result.stream().anyMatch(l -> l.getBlockX() == 5 && l.getBlockY() == 10 && l.getBlockZ() == 15);
        assertTrue(found, "Center block must be in result");
    }

    @Test
    void rhombusRadius1Gives7Blocks() {
        // center + 6 face-adjacent neighbors
        assertEquals(7, BlocksShape.RHOMBUS.collect(center(), 1, 1, 1).size());
    }

    @Test
    void rhombusSmallerThanCuboid() {
        int cuboid = BlocksShape.CUBOID.collect(center(), 2, 2, 2).size();
        int rhombus = BlocksShape.RHOMBUS.collect(center(), 2, 2, 2).size();
        assertTrue(rhombus < cuboid);
    }

    @Test
    void sphereRadius1Gives7Blocks() {
        assertEquals(7, BlocksShape.SPHERE.collect(center(), 1, 1, 1).size());
    }

    @Test
    void sphereSmallerThanCuboid() {
        int cuboid = BlocksShape.CUBOID.collect(center(), 3, 3, 3).size();
        int sphere = BlocksShape.SPHERE.collect(center(), 3, 3, 3).size();
        assertTrue(sphere < cuboid);
    }

    @Test
    void cylinderRadius1Height1Gives15Blocks() {
        // 5 XZ positions × 3 Y heights = 15
        assertEquals(15, BlocksShape.CYLINDER.collect(center(), 1, 1, 1).size());
    }

    @Test
    void cylinderFlatDiscHasOneLevelHeight() {
        // ry=0 means only dy=0 → same as XZ disc
        List<Location> flat = BlocksShape.CYLINDER.collect(center(), 2, 0, 2);
        List<Location> disc = BlocksShape.CYLINDER.collect(center(), 2, 1, 2);
        // flat has fewer blocks than the full cylinder (which spans 3 heights)
        assertTrue(flat.size() < disc.size());
    }

    @Test
    void coneFallbackToSphere() {
        // CONE.collect delegates to SPHERE.collect(center, rz, rz, rz)
        int coneCount = BlocksShape.CONE.collect(center(), 0, 0, 2).size();
        int sphereCount = BlocksShape.SPHERE.collect(center(), 2, 2, 2).size();
        assertEquals(sphereCount, coneCount);
    }

    @Test
    void pyramidFallbackToCuboid() {
        // PYRAMID.collect delegates to CUBOID.collect
        int pyramidCount = BlocksShape.PYRAMID.collect(center(), 2, 2, 3).size();
        int cuboidCount = BlocksShape.CUBOID.collect(center(), 2, 2, 3).size();
        assertEquals(cuboidCount, pyramidCount);
    }

    @Test
    void beamFallbackToCylinder() {
        // BEAM.collect delegates to CYLINDER.collect
        int beamCount = BlocksShape.BEAM.collect(center(), 1, 1, 3).size();
        int cylinderCount = BlocksShape.CYLINDER.collect(center(), 1, 1, 3).size();
        assertEquals(cylinderCount, beamCount);
    }
}
