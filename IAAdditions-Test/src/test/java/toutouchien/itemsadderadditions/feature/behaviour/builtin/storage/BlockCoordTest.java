package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BlockCoordTest {
    private static ServerMock server;
    private static WorldMock world;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void ofExtractsBlockCoordinates() {
        // Bukkit getBlockX/Y/Z use Math.floor: -5.3 -> -6, not -5
        Location loc = new Location(world, 10.7, 64.0, -5.3);
        BlockCoord coord = BlockCoord.of(loc);

        assertEquals(10, coord.x());
        assertEquals(64, coord.y());
        assertEquals(-6, coord.z());
    }

    @Test
    void ofExtractsWorldName() {
        Location loc = new Location(world, 0, 0, 0);
        BlockCoord coord = BlockCoord.of(loc);
        assertEquals("world", coord.world());
    }

    @Test
    void ofWithNullWorldUsesEmptyString() {
        Location loc = new Location(null, 3, 5, 7);
        BlockCoord coord = BlockCoord.of(loc);
        assertEquals("", coord.world());
    }

    @Test
    void equalCoordsAreEqual() {
        Location loc1 = new Location(world, 1.9, 2.9, 3.9);
        Location loc2 = new Location(world, 1.1, 2.1, 3.1);
        assertEquals(BlockCoord.of(loc1), BlockCoord.of(loc2));
    }

    @Test
    void differentXMakesNotEqual() {
        assertEquals(
                BlockCoord.of(new Location(world, 0, 0, 0)),
                BlockCoord.of(new Location(world, 0, 0, 0))
        );
        assertNotEquals(
                BlockCoord.of(new Location(world, 1, 0, 0)),
                BlockCoord.of(new Location(world, 2, 0, 0))
        );
    }

    @Test
    void locationYawAndPitchIgnored() {
        Location locA = new Location(world, 5, 10, 15, 45f, 30f);
        Location locB = new Location(world, 5, 10, 15, 180f, -90f);
        assertEquals(BlockCoord.of(locA), BlockCoord.of(locB));
    }

    @Test
    void equalCoordsHaveSameHashCode() {
        BlockCoord c1 = new BlockCoord("world", 1, 2, 3);
        BlockCoord c2 = new BlockCoord("world", 1, 2, 3);
        assertEquals(c1.hashCode(), c2.hashCode());
    }
}
