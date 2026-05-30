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

class ProvidesBannerPatternsComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @ParameterizedTest
    @ValueSource(strings = {"minecraft:no_item_required", "#minecraft:no_item_required", "no_item_required"})
    void configureAcceptsTagKeyForms(String raw) {
        // This component builds a TagKey without registry validation, so any well-formed key is accepted.
        assertTrue(new ProvidesBannerPatternsComponent().configure(raw, "test:item"));
    }

    @Test
    void configureRejectsNonString() {
        assertFalse(new ProvidesBannerPatternsComponent().configure(7, "test:item"));
    }

    @Test
    void applySetsProvidesBannerPatterns() {
        ProvidesBannerPatternsComponent component = new ProvidesBannerPatternsComponent();
        assertTrue(component.configure("no_item_required", "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.PAPER), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.PROVIDES_BANNER_PATTERNS));
    }
}
