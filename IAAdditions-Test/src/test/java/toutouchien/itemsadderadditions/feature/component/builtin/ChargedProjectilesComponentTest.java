package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChargedProjectilesComponentTest {
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
    void configureRejectsNonList() {
        assertFalse(new ChargedProjectilesComponent().configure("arrow", "test:item"));
    }

    @Test
    void configureResolvesVanillaItemsAndApplies() {
        ChargedProjectilesComponent component = new ChargedProjectilesComponent();
        assertTrue(component.configure(List.of("arrow", "minecraft:firework_rocket"), "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.CROSSBOW), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.CHARGED_PROJECTILES));
    }

    @Test
    void configureRejectsUnknownItem() {
        assertFalse(new ChargedProjectilesComponent().configure(List.of("not_a_real_item"), "test:item"));
    }

    @Test
    void configureRejectsEmptyList() {
        assertFalse(new ChargedProjectilesComponent().configure(List.of(), "test:item"));
    }
}
