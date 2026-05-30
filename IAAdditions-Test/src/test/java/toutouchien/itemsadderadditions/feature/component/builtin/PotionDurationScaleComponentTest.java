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

class PotionDurationScaleComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 1.5, 255.0})
    void configureAcceptsInRange(double v) {
        assertTrue(new PotionDurationScaleComponent().configure(v, "test:item"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.5, 255.1, 1000.0})
    void configureRejectsOutOfRange(double v) {
        assertFalse(new PotionDurationScaleComponent().configure(v, "test:item"));
    }

    @Test
    void configureRejectsNonNumber() {
        assertFalse(new PotionDurationScaleComponent().configure("x", "test:item"));
    }

    @Test
    void applySetsScale() {
        PotionDurationScaleComponent component = new PotionDurationScaleComponent();
        assertTrue(component.configure(2.0, "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.POTION), "test:item");
        assertEquals(2.0f, stack.getData(DataComponentTypes.POTION_DURATION_SCALE), 0.0001f);
    }
}
