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

class LodestoneTrackerComponentTest {
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

    private static ItemStack compass() {
        return new ItemStack(Material.COMPASS);
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new LodestoneTrackerComponent().configure("nope", "test:item"));
    }

    @Test
    void emptySectionAppliesUnlinkedTracker() {
        LodestoneTrackerComponent component = new LodestoneTrackerComponent();
        assertTrue(component.configure(new YamlConfiguration(), "test:item"));

        ItemStack stack = component.apply(compass(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.LODESTONE_TRACKER));
    }

    @Test
    void unknownWorldStillApplies() {
        LodestoneTrackerComponent component = new LodestoneTrackerComponent();
        // A missing world logs a warning but the tracker is still applied without a location.
        assertTrue(component.configure(yamlOf("world: no_such_world\nx: 10\ny: 64\nz: -5"), "test:item"));

        ItemStack stack = component.apply(compass(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.LODESTONE_TRACKER));
    }
}
