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
import toutouchien.itemsadderadditions.common.version.VersionUtils;

import static org.junit.jupiter.api.Assertions.*;

class MinimumAttackChargeComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void requiresMinimumVersion() {
        assertEquals(VersionUtils.v1_21_1, new MinimumAttackChargeComponent().minimumVersion());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 1.0})
    void configureAcceptsInRange(double v) {
        assertTrue(new MinimumAttackChargeComponent().configure(v, "test:item"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.1, 5.0})
    void configureRejectsOutOfRange(double v) {
        assertFalse(new MinimumAttackChargeComponent().configure(v, "test:item"));
    }

    @Test
    void configureRejectsNonNumber() {
        assertFalse(new MinimumAttackChargeComponent().configure("half", "test:item"));
    }

    @Test
    void applySetsCharge() {
        MinimumAttackChargeComponent component = new MinimumAttackChargeComponent();
        assertTrue(component.configure(0.25, "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.DIAMOND_SWORD), "test:item");
        assertEquals(0.25f, stack.getData(DataComponentTypes.MINIMUM_ATTACK_CHARGE), 0.0001f);
    }
}
