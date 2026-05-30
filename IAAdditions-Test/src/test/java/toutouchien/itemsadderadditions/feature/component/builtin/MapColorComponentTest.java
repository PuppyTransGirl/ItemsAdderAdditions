package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class MapColorComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ItemStack map() {
        return new ItemStack(Material.FILLED_MAP);
    }

    @Test
    void configureAcceptsHexColor() {
        assertTrue(new MapColorComponent().configure("#00AAFF", "test:item"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00AAFF", "#FF", "#0011223", "blue"})
    void configureRejectsMalformedHex(String raw) {
        assertFalse(new MapColorComponent().configure(raw, "test:item"));
    }

    @Test
    void configureRejectsInvalidHexDigits() {
        assertFalse(new MapColorComponent().configure("#ZZ0011", "test:item"));
    }

    @Test
    void configureRejectsNonString() {
        assertFalse(new MapColorComponent().configure(100, "test:item"));
    }

    @Test
    void applySetsMapColor() {
        MapColorComponent component = new MapColorComponent();
        assertTrue(component.configure("#445566", "test:item"));

        ItemStack stack = component.apply(map(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.MAP_COLOR));
    }

    @Test
    void applyWithoutConfigureLeavesDataUntouched() {
        ItemStack stack = map();
        ItemStack result = new MapColorComponent().apply(stack, "test:item");

        assertSame(stack, result);
        assertNull(result.getData(DataComponentTypes.MAP_COLOR));
    }
}
