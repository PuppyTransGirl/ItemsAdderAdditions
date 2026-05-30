package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Enchantable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class EnchantableComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 15, 255})
    void configureAcceptsInRangeValues(int value) {
        assertTrue(new EnchantableComponent().configure(value, "test:item"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 256, 1000})
    void configureRejectsOutOfRangeValues(int value) {
        assertFalse(new EnchantableComponent().configure(value, "test:item"));
    }

    @Test
    void configureRejectsNonNumber() {
        assertFalse(new EnchantableComponent().configure("high", "test:item"));
    }

    @Test
    void applySetsEnchantableData() {
        EnchantableComponent component = new EnchantableComponent();
        component.configure(10, "test:item");

        ItemStack stack = component.apply(ItemStack.of(Material.DIAMOND_SWORD), "test:item");

        assertEquals(Enchantable.enchantable(10), stack.getData(DataComponentTypes.ENCHANTABLE));
    }
}
