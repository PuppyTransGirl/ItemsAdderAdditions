package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.OminousBottleAmplifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class OminousBottleAmplifierComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ItemStack bottle() {
        return ItemStack.of(Material.OMINOUS_BOTTLE);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 4})
    void configureAcceptsInRangeValues(int amplifier) {
        assertTrue(new OminousBottleAmplifierComponent().configure(amplifier, "test:item"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 5, 100})
    void configureRejectsOutOfRangeValues(int amplifier) {
        assertFalse(new OminousBottleAmplifierComponent().configure(amplifier, "test:item"));
    }

    @Test
    void configureRejectsNonNumber() {
        assertFalse(new OminousBottleAmplifierComponent().configure("two", "test:item"));
    }

    @Test
    void applySetsAmplifierData() {
        OminousBottleAmplifierComponent component = new OminousBottleAmplifierComponent();
        component.configure(3, "test:item");

        ItemStack stack = component.apply(bottle(), "test:item");

        assertEquals(OminousBottleAmplifier.amplifier(3),
                stack.getData(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER));
    }
}
