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

class RepairableComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ItemStack sword() {
        return new ItemStack(Material.DIAMOND_SWORD);
    }

    // NOTE: the item-tag branch (e.g. "#minecraft:planks") cannot be asserted positively here
    // because MockBukkit's Registry.ITEM.getTag(...) returns null for every tag, which the
    // component treats as "tag not found". The item-list branch is fully exercised below.

    @Test
    void configureAcceptsItemListAndApplies() {
        RepairableComponent component = new RepairableComponent();
        assertTrue(component.configure(List.of("minecraft:diamond", "emerald"), "test:item"));

        ItemStack stack = component.apply(sword(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.REPAIRABLE));
    }

    @Test
    void configureRejectsUnknownTag() {
        assertFalse(new RepairableComponent().configure("#minecraft:not_a_real_tag", "test:item"));
    }

    @Test
    void configureRejectsEmptyList() {
        assertFalse(new RepairableComponent().configure(List.of(), "test:item"));
    }

    @Test
    void configureRejectsWrongType() {
        assertFalse(new RepairableComponent().configure(42, "test:item"));
    }
}
