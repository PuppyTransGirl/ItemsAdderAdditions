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

class MapDecorationsComponentTest {
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
        assertFalse(new MapDecorationsComponent().configure("marker", "test:item"));
    }

    @Test
    void configureAcceptsValidDecorationAndApplies() {
        MapDecorationsComponent component = new MapDecorationsComponent();
        assertTrue(component.configure(yamlOf("""
                home:
                  type: player
                  x: 100.0
                  z: 200.0
                  rotation: 90.0
                """), "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.FILLED_MAP), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.MAP_DECORATIONS));
    }

    @Test
    void configureRejectsUnknownDecorationType() {
        assertFalse(new MapDecorationsComponent().configure(yamlOf("bad:\n  type: not_a_marker"), "test:item"));
    }

    @Test
    void configureRejectsEmptySection() {
        assertFalse(new MapDecorationsComponent().configure(new YamlConfiguration(), "test:item"));
    }
}
