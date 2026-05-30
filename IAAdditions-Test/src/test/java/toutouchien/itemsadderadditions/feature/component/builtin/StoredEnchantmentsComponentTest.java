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

class StoredEnchantmentsComponentTest {
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
        assertFalse(new StoredEnchantmentsComponent().configure("sharpness", "test:item"));
    }

    @Test
    void configureAcceptsEnchantmentsAndApplies() {
        StoredEnchantmentsComponent component = new StoredEnchantmentsComponent();
        assertTrue(component.configure(yamlOf("sharpness: 5\nunbreaking: 3"), "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.ENCHANTED_BOOK), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.STORED_ENCHANTMENTS));
    }

    @Test
    void configureRejectsUnknownEnchantment() {
        assertFalse(new StoredEnchantmentsComponent().configure(yamlOf("megaslash: 1"), "test:item"));
    }

    @Test
    void configureRejectsLevelOutOfRange() {
        assertFalse(new StoredEnchantmentsComponent().configure(yamlOf("sharpness: 0"), "test:item"));
        assertFalse(new StoredEnchantmentsComponent().configure(yamlOf("sharpness: 256"), "test:item"));
    }

    @Test
    void configureRejectsEmptySection() {
        assertFalse(new StoredEnchantmentsComponent().configure(new YamlConfiguration(), "test:item"));
    }
}
