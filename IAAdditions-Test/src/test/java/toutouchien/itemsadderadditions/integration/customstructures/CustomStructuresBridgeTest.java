package toutouchien.itemsadderadditions.integration.customstructures;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomStructuresBridgeTest {
    private static ServerMock server;
    private static WorldMock world;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @Test
    void storesBoundsAcrossChunksAndMatchesNamesCaseInsensitively() {
        CustomStructuresBridge.storeBounds(
                "Ancient_Temple",
                new Location(world, 0, 64, 0),
                new Location(world, -2, 60, -2),
                new Location(world, 20, 75, 20),
                0
        );

        assertTrue(CustomStructuresBridge.isInStructure(
                new Location(world, 18, 70, 18), "ancient_temple"
        ));
        assertTrue(CustomStructuresBridge.isInStructure(
                new Location(world, -2, 60, -2), "ANCIENT_TEMPLE"
        ));
        assertFalse(CustomStructuresBridge.isInStructure(
                new Location(world, 21, 70, 18), "ancient_temple"
        ));
        assertFalse(CustomStructuresBridge.isInStructure(
                new Location(world, 18, 70, 18), "other_structure"
        ));
    }

    @Test
    void appliesSpawnRotationToSchematicBounds() {
        CustomStructuresBridge.storeBounds(
                "rotated",
                new Location(world, 100, 64, 100),
                new Location(world, 100, 60, 100),
                new Location(world, 115, 75, 131),
                90
        );

        assertTrue(CustomStructuresBridge.isInStructure(
                new Location(world, 131, 70, 85), "rotated"
        ));
        assertFalse(CustomStructuresBridge.isInStructure(
                new Location(world, 115, 70, 131), "rotated"
        ));
    }
}
