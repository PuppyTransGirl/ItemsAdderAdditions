package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class ProvidesTrimMaterialComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void configureAcceptsKnownMaterialUnqualified() {
        assertTrue(new ProvidesTrimMaterialComponent().configure("gold", "test:item"));
    }

    @Test
    void configureRejectsUnknownMaterial() {
        assertFalse(new ProvidesTrimMaterialComponent().configure("not_a_trim_material", "test:item"));
    }

    @Test
    void configureRejectsNonString() {
        assertFalse(new ProvidesTrimMaterialComponent().configure(3, "test:item"));
    }

    @Test
    void applySetsTrimMaterial() {
        ProvidesTrimMaterialComponent component = new ProvidesTrimMaterialComponent();
        assertTrue(component.configure("gold", "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.DIAMOND_CHESTPLATE), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.PROVIDES_TRIM_MATERIAL));
    }
}
