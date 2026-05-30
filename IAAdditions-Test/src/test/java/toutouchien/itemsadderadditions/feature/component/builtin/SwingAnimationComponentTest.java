package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.common.version.VersionUtils;

import static org.junit.jupiter.api.Assertions.*;

class SwingAnimationComponentTest {
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
    void requiresMinimumVersion() {
        assertEquals(VersionUtils.v1_21_5, new SwingAnimationComponent().minimumVersion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NONE", "WHACK", "STAB", "whack"})
    void configureAcceptsValidTypes(String type) {
        assertTrue(new SwingAnimationComponent().configure(yamlOf("type: " + type + "\nduration: 6"), "test:item"));
    }

    @Test
    void configureRejectsUnknownType() {
        assertFalse(new SwingAnimationComponent().configure(yamlOf("type: SPIN"), "test:item"));
    }

    @Test
    void configureRejectsDurationBelowOne() {
        assertFalse(new SwingAnimationComponent().configure(yamlOf("type: WHACK\nduration: 0"), "test:item"));
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new SwingAnimationComponent().configure("WHACK", "test:item"));
    }

    @Test
    void applySetsSwingAnimation() {
        SwingAnimationComponent component = new SwingAnimationComponent();
        assertTrue(component.configure(yamlOf("type: STAB\nduration: 8"), "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.DIAMOND_SWORD), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.SWING_ANIMATION));
    }
}
