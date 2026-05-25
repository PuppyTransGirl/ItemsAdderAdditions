package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageType;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.session.StorageSessionRegistry;

import static org.junit.jupiter.api.Assertions.*;

class StorageInventoryResolverTest {
    private static ServerMock server;
    private static JavaPlugin plugin;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static StorageInventoryResolver resolver(int rows, StorageInventorySpec spec, StorageType type) {
        return new StorageInventoryResolver(
                new StorageSessionRegistry(),
                rows,
                spec,
                Component.text("Storage"),
                type,
                new NamespacedKey("itemsadderadditions", "contents"),
                plugin
        );
    }

    @Test
    void liveContentsAtReturnsNullWhenNoSessionExistsAtLocation() {
        StorageInventoryResolver resolver = resolver(3, null, StorageType.STORAGE);

        assertNull(resolver.liveContentsAt(new Location(server.getWorld("world"), 0, 64, 0)));
    }

    @Test
    void openForDisposalCreatesSizedInventoryWithoutStoredContents() {
        PlayerMock player = server.addPlayer();
        Location location = new Location(server.getWorld("world"), 1, 65, 1);
        StorageInventoryResolver resolver = resolver(2, null, StorageType.DISPOSAL);

        Inventory inventory = resolver.openFor(player, location, null, null);

        assertEquals(18, inventory.getSize());
        assertInstanceOf(StorageInventoryHolder.class, inventory.getHolder());
        assertEquals(location, ((StorageInventoryHolder) inventory.getHolder()).location());
    }

    @Test
    void openForTypedInventoryUsesTypedSpec() {
        PlayerMock player = server.addPlayer();
        Location location = new Location(server.getWorld("world"), 2, 65, 2);
        StorageInventoryResolver resolver = resolver(
                3,
                new StorageInventorySpec.Typed(InventoryType.DROPPER),
                StorageType.DISPOSAL
        );

        Inventory inventory = resolver.openFor(player, location, null, null);

        assertEquals(9, inventory.getSize());
        assertEquals(InventoryType.DROPPER, inventory.getType());
        assertInstanceOf(StorageInventoryHolder.class, inventory.getHolder());
    }
}
