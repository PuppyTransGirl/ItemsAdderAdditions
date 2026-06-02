package toutouchien.itemsadderadditions.testsupport;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static toutouchien.itemsadderadditions.testsupport.MockBukkitUnsupported.failInsteadOfSkip;

class MockBukkitUnsupportedApiTest {
    @BeforeEach
    void setup() {
        MockBukkit.mock();
    }

    @AfterEach
    void teardown() {
        MockBukkit.unmock();
    }

    @Test
    @Disabled("MockBukkit InventoryMock.getHolder(boolean) is not implemented")
    void inventoryGetHolderBooleanShouldReturnOriginalHolder() {
        InventoryHolder holder = mock(InventoryHolder.class);
        Inventory inventory = Bukkit.createInventory(holder, 9);

        failInsteadOfSkip(() -> assertSame(holder, inventory.getHolder(false)));
    }

    @Test
    @Disabled("MockBukkit BlockDataMock does not implement Tripwire")
    void tripwireBlockDataShouldImplementBukkitTripwireType() {
        BlockData blockData = Bukkit.createBlockData(Material.TRIPWIRE);

        assertInstanceOf(Tripwire.class, blockData);
    }
}
