package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.common.version.VersionUtils;

import static org.junit.jupiter.api.Assertions.*;

class WeaponComponentTest {
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
        assertEquals(VersionUtils.v1_21_5, new WeaponComponent().minimumVersion());
    }

    @Test
    void emptyConfigUsesDefaults() {
        WeaponComponent component = new WeaponComponent();
        assertTrue(component.configure(new YamlConfiguration(), "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.DIAMOND_SWORD), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.WEAPON));
    }

    @Test
    void customValuesAreInjectedAndApplied() {
        WeaponComponent component = new WeaponComponent();
        assertTrue(component.configure(yamlOf("item_damage_per_attack: 3\ndisable_blocking_for_seconds: 2.0"), "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.DIAMOND_SWORD), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.WEAPON));
    }
}
