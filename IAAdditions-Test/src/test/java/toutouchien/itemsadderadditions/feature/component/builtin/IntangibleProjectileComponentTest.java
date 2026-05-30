package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntangibleProjectileComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void configureTrueEnables() {
        assertTrue(new IntangibleProjectileComponent().configure(true, "test:item"));
    }

    @Test
    void configureFalseDisables() {
        assertFalse(new IntangibleProjectileComponent().configure(false, "test:item"));
    }

    @Test
    void configureNonNullNonBooleanEnables() {
        // The component treats any non-null, non-boolean presence as "enabled".
        assertTrue(new IntangibleProjectileComponent().configure("yes", "test:item"));
    }

    @Test
    void configureNullDisables() {
        assertFalse(new IntangibleProjectileComponent().configure(null, "test:item"));
    }

    @Test
    void applySetsMarkerComponent() {
        IntangibleProjectileComponent component = new IntangibleProjectileComponent();
        component.configure(true, "test:item");

        ItemStack stack = component.apply(ItemStack.of(Material.SNOWBALL), "test:item");
        assertTrue(stack.hasData(DataComponentTypes.INTANGIBLE_PROJECTILE));
    }
}
