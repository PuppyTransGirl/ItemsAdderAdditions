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

class FireworksComponentTest {
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
    void configureAcceptsFlightDurationOnly() {
        assertTrue(new FireworksComponent().configure(sec(Map.of("flight_duration", 2)), "test:item"));
    }

    @Test
    void configureAcceptsExplosionEntries() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("flight_duration", 2);
        root.set("explosions", List.of(sec(Map.of("type", "BALL", "colors", List.of("#FF0000")))));

        assertTrue(new FireworksComponent().configure(root, "test:item"));
    }

    @Test
    void configureRejectsFlightDurationOutOfRange() {
        assertFalse(new FireworksComponent().configure(sec(Map.of("flight_duration", 300)), "test:item"));
    }

    @Test
    void configureRejectsInvalidExplosionType() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("explosions", List.of(sec(Map.of("type", "PYRAMID"))));

        assertFalse(new FireworksComponent().configure(root, "test:item"));
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new FireworksComponent().configure(2, "test:item"));
    }

    @Test
    void applySetsFireworks() {
        FireworksComponent component = new FireworksComponent();
        assertTrue(component.configure(sec(Map.of("flight_duration", 1)), "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.FIREWORK_ROCKET), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.FIREWORKS));
    }
}
