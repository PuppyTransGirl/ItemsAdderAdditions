package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertSame;

class StorageInventoryHolderTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void holderStoresLocationAndInventory() {
        Location location = new Location(server.getWorld("world"), 3, 64, 7);
        StorageInventoryHolder holder = new StorageInventoryHolder(location);
        Inventory inventory = Bukkit.createInventory(holder, 9);

        holder.inventory(inventory);

        assertSame(location, holder.location());
        assertSame(inventory, holder.getInventory());
        assertSame(holder, inventory.getHolder());
    }
}
