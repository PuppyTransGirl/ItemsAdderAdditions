package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BannerPatternsComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ConfigurationSection sec(Map<String, Object> values) {
        return new YamlConfiguration().createSection("e", values);
    }

    @Test
    void configureRejectsNonList() {
        assertFalse(new BannerPatternsComponent().configure("stripe_top", "test:item"));
    }

    @Test
    void configureAcceptsValidPatternsAndApplies() {
        BannerPatternsComponent component = new BannerPatternsComponent();
        assertTrue(component.configure(List.of(
                sec(Map.of("type", "stripe_top", "color", "RED")),
                sec(Map.of("type", "square_bottom_left", "color", "blue"))), "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.WHITE_BANNER), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.BANNER_PATTERNS));
    }

    @Test
    void configureRejectsUnknownPatternType() {
        assertFalse(new BannerPatternsComponent()
                .configure(List.of(sec(Map.of("type", "not_a_pattern", "color", "RED"))), "test:item"));
    }

    @Test
    void configureRejectsInvalidColor() {
        assertFalse(new BannerPatternsComponent()
                .configure(List.of(sec(Map.of("type", "stripe_top", "color", "rainbow"))), "test:item"));
    }

    @Test
    void configureRejectsEmptyList() {
        assertFalse(new BannerPatternsComponent().configure(List.of(), "test:item"));
    }
}
