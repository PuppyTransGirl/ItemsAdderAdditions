package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapId;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class MapIdComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void configureAcceptsInteger() {
        assertTrue(new MapIdComponent().configure(7, "test:item"));
    }

    @Test
    void configureRejectsNonNumber() {
        assertFalse(new MapIdComponent().configure("seven", "test:item"));
    }

    @Test
    void applySetsMapId() {
        MapIdComponent component = new MapIdComponent();
        assertTrue(component.configure(42, "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.FILLED_MAP), "test:item");
        assertEquals(MapId.mapId(42), stack.getData(DataComponentTypes.MAP_ID));
    }
}
