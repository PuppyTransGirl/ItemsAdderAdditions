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

class FireworkExplosionComponentTest {
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
    void configureAcceptsValidEffect() {
        assertTrue(new FireworkExplosionComponent().configure(
                yamlOf("type: STAR\ncolors:\n  - '#FF0000'\nfade_colors:\n  - '#00FF00'\ntrail: true\nflicker: true"),
                "test:item"));
    }

    @Test
    void configureDefaultsTypeToBall() {
        assertTrue(new FireworkExplosionComponent().configure(yamlOf("colors:\n  - '#FFFFFF'"), "test:item"));
    }

    @Test
    void configureRejectsUnknownType() {
        assertFalse(new FireworkExplosionComponent().configure(yamlOf("type: PYRAMID"), "test:item"));
    }

    @Test
    void configureRejectsInvalidColorHex() {
        assertFalse(new FireworkExplosionComponent().configure(yamlOf("type: BALL\ncolors:\n  - 'red'"), "test:item"));
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new FireworkExplosionComponent().configure("BALL", "test:item"));
    }

    @Test
    void applySetsFireworkExplosion() {
        FireworkExplosionComponent component = new FireworkExplosionComponent();
        assertTrue(component.configure(yamlOf("type: BURST\ncolors:\n  - '#123456'"), "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.FIREWORK_STAR), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.FIREWORK_EXPLOSION));
    }
}
