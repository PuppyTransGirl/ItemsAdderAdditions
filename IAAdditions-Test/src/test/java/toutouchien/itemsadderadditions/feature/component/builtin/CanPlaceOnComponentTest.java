package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CanPlaceOnComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void configureRejectsNonList() {
        assertFalse(new CanPlaceOnComponent().configure("minecraft:stone", "test:item"));
    }

    @Test
    void configureAcceptsBlockIdsAndApplies() {
        CanPlaceOnComponent component = new CanPlaceOnComponent();
        assertTrue(component.configure(List.of("minecraft:stone"), "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.TORCH), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.CAN_PLACE_ON));
    }

    @Test
    void configureRejectsUnknownTag() {
        assertFalse(new CanPlaceOnComponent().configure(List.of("#minecraft:not_a_real_tag"), "test:item"));
    }

    @Test
    void configureRejectsEmptyList() {
        assertFalse(new CanPlaceOnComponent().configure(List.of(), "test:item"));
    }
}
