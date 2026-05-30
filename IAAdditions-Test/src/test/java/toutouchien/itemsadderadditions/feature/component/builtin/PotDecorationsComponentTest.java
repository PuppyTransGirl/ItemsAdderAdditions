package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class PotDecorationsComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new PotDecorationsComponent().configure("brick", "test:item"));
    }

    @Test
    void configureAcceptsValidItemsAndApplies() {
        PotDecorationsComponent component = new PotDecorationsComponent();
        assertTrue(component.configure(yamlOf("back: brick\nfront: arms_up_pottery_sherd"), "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.DECORATED_POT), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.POT_DECORATIONS));
    }

    @Test
    void unknownItemTypeIsTolerated() {
        // resolveItemType logs a warning and yields null for unknown faces; configure still succeeds.
        assertTrue(new PotDecorationsComponent().configure(yamlOf("back: not_a_real_item"), "test:item"));
    }
}
