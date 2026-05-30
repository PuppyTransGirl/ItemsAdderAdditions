package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class RarityComponentTest {
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

    @Test
    void configureAcceptsValidRarity() {
        RarityComponent component = new RarityComponent();
        assertTrue(component.configure("RARE", "test:item"));
    }

    @Test
    void configureIsCaseInsensitive() {
        RarityComponent component = new RarityComponent();
        assertTrue(component.configure("epic", "test:item"));

        ItemStack stack = component.apply(sword(), "test:item");
        assertEquals(ItemRarity.EPIC, stack.getData(DataComponentTypes.RARITY));
    }

    @Test
    void configureRejectsUnknownValue() {
        assertFalse(new RarityComponent().configure("LEGENDARY", "test:item"));
    }

    @Test
    void configureRejectsNonString() {
        assertFalse(new RarityComponent().configure(42, "test:item"));
    }

    @Test
    void configureRejectsNull() {
        assertFalse(new RarityComponent().configure(null, "test:item"));
    }

    @Test
    void applySetsRarityData() {
        RarityComponent component = new RarityComponent();
        component.configure("UNCOMMON", "test:item");

        ItemStack stack = component.apply(sword(), "test:item");

        assertEquals(ItemRarity.UNCOMMON, stack.getData(DataComponentTypes.RARITY));
    }

    @Test
    void applyWithoutConfigureLeavesRarityUntouched() {
        ItemStack stack = sword();
        ItemRarity before = stack.getData(DataComponentTypes.RARITY);

        ItemStack result = new RarityComponent().apply(stack, "test:item");

        assertSame(stack, result);
        assertEquals(before, result.getData(DataComponentTypes.RARITY));
    }
}
