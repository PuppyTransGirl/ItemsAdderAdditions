package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import static org.junit.jupiter.api.Assertions.*;

class UseRemainderComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
        NamespaceUtils.initVanillaCache();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void configureRejectsNonString() {
        assertFalse(new UseRemainderComponent().configure(42, "test:item"));
    }

    @Test
    void configureResolvesVanillaItemAndApplies() {
        UseRemainderComponent component = new UseRemainderComponent();
        assertTrue(component.configure("glass_bottle", "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.POTION), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.USE_REMAINDER));
    }

    @Test
    void configureRejectsUnknownItem() {
        assertFalse(new UseRemainderComponent().configure("not_a_real_item", "test:item"));
    }
}
