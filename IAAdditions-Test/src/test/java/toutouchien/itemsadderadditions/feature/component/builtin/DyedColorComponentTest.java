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

class DyedColorComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ItemStack armor() {
        return new ItemStack(Material.LEATHER_CHESTPLATE);
    }

    @Test
    void configureAcceptsHexColor() {
        assertTrue(new DyedColorComponent().configure("#FF8000", "test:item"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FF0000", "#FFF", "#FF00000", "red"})
    void configureRejectsMalformedHex(String raw) {
        assertFalse(new DyedColorComponent().configure(raw, "test:item"));
    }

    @Test
    void configureRejectsInvalidHexDigits() {
        assertFalse(new DyedColorComponent().configure("#GGGGGG", "test:item"));
    }

    @Test
    void configureRejectsNonString() {
        assertFalse(new DyedColorComponent().configure(255, "test:item"));
    }

    @Test
    void applySetsDyedColor() {
        DyedColorComponent component = new DyedColorComponent();
        assertTrue(component.configure("#123456", "test:item"));

        ItemStack stack = component.apply(armor(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.DYED_COLOR));
    }

    @Test
    void applyWithoutConfigureLeavesDataUntouched() {
        ItemStack stack = armor();
        ItemStack result = new DyedColorComponent().apply(stack, "test:item");

        assertSame(stack, result);
        assertNull(result.getData(DataComponentTypes.DYED_COLOR));
    }
}
