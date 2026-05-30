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

class CanBreakComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ItemStack pick() {
        return ItemStack.of(Material.DIAMOND_PICKAXE);
    }

    @Test
    void configureRejectsNonList() {
        assertFalse(new CanBreakComponent().configure("minecraft:stone", "test:item"));
    }

    @Test
    void configureAcceptsBlockIdsAndApplies() {
        CanBreakComponent component = new CanBreakComponent();
        assertTrue(component.configure(List.of("minecraft:stone", "dirt"), "test:item"));

        ItemStack stack = component.apply(pick(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.CAN_BREAK));
    }

    // NOTE: the positive block-tag branch (e.g. "#minecraft:logs") is not asserted here:
    // MockBukkit's Registry.BLOCK.getTag(...) returns null for every tag, so a "valid" tag
    // is indistinguishable from an unknown one under the mock.

    @Test
    void configureRejectsUnknownTag() {
        assertFalse(new CanBreakComponent().configure(List.of("#minecraft:not_a_real_tag"), "test:item"));
    }

    @Test
    void configureRejectsEmptyList() {
        assertFalse(new CanBreakComponent().configure(List.of(), "test:item"));
    }
}
