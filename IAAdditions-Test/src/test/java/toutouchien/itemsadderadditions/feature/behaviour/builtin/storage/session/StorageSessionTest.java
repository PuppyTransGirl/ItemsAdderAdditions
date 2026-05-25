package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.session;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageType;

import static org.junit.jupiter.api.Assertions.*;

class StorageSessionTest {
    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;
    private Inventory inventory;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        inventory = Bukkit.createInventory(null, 9);
    }

    @AfterEach
    void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void blockSession_holderLocationUsesBlockLocation() {
        Block block = world.getBlockAt(4, 65, -3);
        StorageSession session = new StorageSession(player, inventory, block, null, StorageType.STORAGE);

        Location loc = session.holderLocation();

        assertEquals(block.getLocation(), loc);
    }

    @Test
    void blockSession_isBlockTrueAndFurnitureFalse() {
        StorageSession session = new StorageSession(player, inventory, world.getBlockAt(0, 64, 0), null, StorageType.SHULKER);

        assertTrue(session.isBlock());
        assertFalse(session.isFurniture());
    }

    @Test
    void furnitureSession_holderLocationUsesEntityLocation() {
        PlayerMock entity = server.addPlayer();
        Location location = new Location(world, 8.5, 70, -2.5);
        entity.setLocation(location);
        StorageSession session = new StorageSession(player, inventory, null, entity, StorageType.DISPOSAL);

        assertEquals(location, session.holderLocation());
    }

    @Test
    void furnitureSession_isFurnitureTrueAndBlockFalse() {
        PlayerMock entity = server.addPlayer();
        StorageSession session = new StorageSession(player, inventory, null, entity, StorageType.STORAGE);

        assertFalse(session.isBlock());
        assertTrue(session.isFurniture());
    }

    @Test
    void holderLocationWithNoHolder_throws() {
        StorageSession session = new StorageSession(player, inventory, null, null, StorageType.STORAGE);

        assertThrows(IllegalStateException.class, session::holderLocation);
    }
}
