package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class BaseColorComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ItemStack shield() {
        return ItemStack.of(Material.SHIELD);
    }

    @Test
    void configureAcceptsValidColorCaseInsensitive() {
        BaseColorComponent component = new BaseColorComponent();
        assertTrue(component.configure("red", "test:item"));

        ItemStack stack = component.apply(shield(), "test:item");
        assertEquals(DyeColor.RED, stack.getData(DataComponentTypes.BASE_COLOR));
    }

    @Test
    void configureRejectsUnknownColor() {
        assertFalse(new BaseColorComponent().configure("NOT_A_COLOR", "test:item"));
    }

    @Test
    void configureRejectsNonString() {
        assertFalse(new BaseColorComponent().configure(42, "test:item"));
    }

    @Test
    void applyWithoutConfigureLeavesDataUntouched() {
        ItemStack stack = shield();
        DyeColor before = stack.getData(DataComponentTypes.BASE_COLOR);

        ItemStack result = new BaseColorComponent().apply(stack, "test:item");

        assertSame(stack, result);
        assertEquals(before, result.getData(DataComponentTypes.BASE_COLOR));
    }
}
