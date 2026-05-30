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

class BundleContentsComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
        // Populates the vanilla item lookup so itemByID can resolve Minecraft items.
        NamespaceUtils.initVanillaCache();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void configureRejectsNonList() {
        assertFalse(new BundleContentsComponent().configure("stone", "test:item"));
    }

    @Test
    void configureResolvesVanillaItemsAndApplies() {
        BundleContentsComponent component = new BundleContentsComponent();
        assertTrue(component.configure(List.of("stone", "minecraft:dirt"), "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.BUNDLE), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.BUNDLE_CONTENTS));
    }

    @Test
    void configureRejectsUnknownItem() {
        assertFalse(new BundleContentsComponent().configure(List.of("not_a_real_item"), "test:item"));
    }

    @Test
    void configureRejectsEmptyList() {
        assertFalse(new BundleContentsComponent().configure(List.of(), "test:item"));
    }
}
