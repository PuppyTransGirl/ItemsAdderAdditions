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

class DamageResistantComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    // NOTE: apply() is intentionally not exercised here. MockBukkit's
    // DamageResistant mock does not implement the damageResistant(TagKey) factory,
    // so only the configure()/tag-normalization path is covered.

    @ParameterizedTest
    @ValueSource(strings = {"minecraft:is_fire", "#minecraft:is_fire", "is_fire", "#is_fire", "IS_FIRE", "  is_fire  "})
    void configureAcceptsTagKeyForms(String raw) {
        assertTrue(new DamageResistantComponent().configure(raw, "test:item"));
    }

    @Test
    void configureRejectsNonString() {
        assertFalse(new DamageResistantComponent().configure(123, "test:item"));
    }

    @Test
    void configureRejectsNull() {
        assertFalse(new DamageResistantComponent().configure(null, "test:item"));
    }

    @Test
    void applyWithoutConfigureLeavesDataUntouched() {
        ItemStack stack = ItemStack.of(Material.DIAMOND_SWORD);
        ItemStack result = new DamageResistantComponent().apply(stack, "test:item");

        // No tag was configured, so apply() must not touch the item or invoke the factory.
        assertSame(stack, result);
        assertNull(result.getData(DataComponentTypes.DAMAGE_RESISTANT));
    }
}
