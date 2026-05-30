package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class DamageTypeComponentTest {
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
    void configureAcceptsKnownDamageType() {
        assertTrue(new DamageTypeComponent().configure("minecraft:on_fire", "test:item"));
    }

    @Test
    void configurePrefixesMinecraftNamespace() {
        // A bare id gets the minecraft: namespace, so this must resolve like the qualified form.
        assertTrue(new DamageTypeComponent().configure("on_fire", "test:item"));
    }

    @Test
    void configureRejectsUnknownDamageType() {
        assertFalse(new DamageTypeComponent().configure("minecraft:definitely_not_real", "test:item"));
    }

    @Test
    void configureRejectsNonString() {
        assertFalse(new DamageTypeComponent().configure(1, "test:item"));
    }

    @Test
    void applySetsDamageType() {
        DamageTypeComponent component = new DamageTypeComponent();
        assertTrue(component.configure("on_fire", "test:item"));

        ItemStack stack = component.apply(sword(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.DAMAGE_TYPE));
    }
}
