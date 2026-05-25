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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StorageSessionRegistryTest {
    private ServerMock server;
    private WorldMock world;
    private WorldMock otherWorld;
    private Inventory inventory;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        otherWorld = server.addSimpleWorld("other");
        inventory = Bukkit.createInventory(null, 9);
    }

    @AfterEach
    void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void liveInventoryAtMissingLocation_returnsNull() {
        StorageSessionRegistry registry = new StorageSessionRegistry();

        assertNull(registry.liveInventoryAt(new Location(world, 0, 64, 0)));
    }

    @Test
    void add_thenLiveInventoryAtSameBlock_returnsInventory() {
        StorageSessionRegistry registry = new StorageSessionRegistry();
        Block block = world.getBlockAt(10, 64, 10);
        registry.add(sessionAt(block));

        assertSame(inventory, registry.liveInventoryAt(block.getLocation()));
    }

    @Test
    void liveInventoryAtNearbySameBlockLocation_matchesWithinTolerance() {
        StorageSessionRegistry registry = new StorageSessionRegistry();
        Block block = world.getBlockAt(10, 64, 10);
        registry.add(sessionAt(block));

        assertSame(inventory, registry.liveInventoryAt(new Location(world, 10.05, 64, 10.05)));
    }

    @Test
    void liveInventoryAtSameCoordinatesDifferentWorld_returnsNull() {
        StorageSessionRegistry registry = new StorageSessionRegistry();
        registry.add(sessionAt(world.getBlockAt(10, 64, 10)));

        assertNull(registry.liveInventoryAt(new Location(otherWorld, 10, 64, 10)));
    }

    @Test
    void hasAtReturnsTrueForOpenLocation() {
        StorageSessionRegistry registry = new StorageSessionRegistry();
        Block block = world.getBlockAt(1, 65, 1);
        registry.add(sessionAt(block));

        assertTrue(registry.hasAt(block.getLocation()));
    }

    @Test
    void hasAtReturnsFalseForFarLocation() {
        StorageSessionRegistry registry = new StorageSessionRegistry();
        registry.add(sessionAt(world.getBlockAt(1, 65, 1)));

        assertFalse(registry.hasAt(new Location(world, 100, 65, 100)));
    }

    @Test
    void nearReturnsSessionsWithinDistance() {
        StorageSessionRegistry registry = new StorageSessionRegistry();
        StorageSession close = sessionAt(world.getBlockAt(0, 64, 0));
        StorageSession far = sessionAt(world.getBlockAt(10, 64, 10));
        registry.add(close);
        registry.add(far);

        assertEquals(java.util.List.of(close), registry.near(new Location(world, 0, 64, 0), 4.0));
    }

    @Test
    void nearIgnoresDifferentWorld() {
        StorageSessionRegistry registry = new StorageSessionRegistry();
        registry.add(sessionAt(otherWorld.getBlockAt(0, 64, 0)));

        assertTrue(registry.near(new Location(world, 0, 64, 0), 100.0).isEmpty());
    }

    @Test
    void addSecondSessionForSamePlayerReplacesFirst() {
        StorageSessionRegistry registry = new StorageSessionRegistry();
        PlayerMock player = server.addPlayer();
        StorageSession first = sessionAt(player, world.getBlockAt(0, 64, 0));
        StorageSession second = sessionAt(player, world.getBlockAt(5, 64, 5));

        registry.add(first);
        registry.add(second);

        assertEquals(java.util.List.of(second), registry.all());
        assertNull(registry.liveInventoryAt(first.holderLocation()));
        assertSame(inventory, registry.liveInventoryAt(second.holderLocation()));
    }

    @Test
    void removeExistingSessionReturnsItAndRemovesIt() {
        StorageSessionRegistry registry = new StorageSessionRegistry();
        StorageSession session = sessionAt(world.getBlockAt(0, 64, 0));
        UUID uuid = session.player().getUniqueId();
        registry.add(session);

        assertSame(session, registry.remove(uuid));
        assertTrue(registry.all().isEmpty());
    }

    @Test
    void removeMissingSessionReturnsNull() {
        StorageSessionRegistry registry = new StorageSessionRegistry();

        assertNull(registry.remove(UUID.randomUUID()));
    }

    @Test
    void allReturnsDefensiveCopy() {
        StorageSessionRegistry registry = new StorageSessionRegistry();
        registry.add(sessionAt(world.getBlockAt(0, 64, 0)));

        assertThrows(UnsupportedOperationException.class, () -> registry.all().clear());
        assertEquals(1, registry.all().size());
    }

    @Test
    void clearRemovesAllSessions() {
        StorageSessionRegistry registry = new StorageSessionRegistry();
        registry.add(sessionAt(world.getBlockAt(0, 64, 0)));
        registry.add(sessionAt(world.getBlockAt(1, 64, 1)));

        registry.clear();

        assertTrue(registry.all().isEmpty());
    }

    private StorageSession sessionAt(Block block) {
        return sessionAt(server.addPlayer(), block);
    }

    private StorageSession sessionAt(PlayerMock player, Block block) {
        return new StorageSession(player, inventory, block, null, StorageType.STORAGE);
    }
}
