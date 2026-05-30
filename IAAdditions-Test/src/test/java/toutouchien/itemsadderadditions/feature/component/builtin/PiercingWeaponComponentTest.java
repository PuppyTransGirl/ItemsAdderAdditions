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

class PiercingWeaponComponentTest {
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
        assertEquals(VersionUtils.v1_21_5, new PiercingWeaponComponent().minimumVersion());
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new PiercingWeaponComponent().configure(123, "test:item"));
    }

    @Test
    void emptyConfigUsesDefaultsAndApplies() {
        PiercingWeaponComponent component = new PiercingWeaponComponent();
        assertTrue(component.configure(new YamlConfiguration(), "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.DIAMOND_SWORD), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.PIERCING_WEAPON));
    }

    @Test
    void fullConfigApplies() {
        PiercingWeaponComponent component = new PiercingWeaponComponent();
        assertTrue(component.configure(yamlOf("""
                deals_knockback: false
                dismounts: false
                sound: minecraft:entity.arrow.hit
                hit_sound: entity.arrow.hit
                """), "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.DIAMOND_SWORD), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.PIERCING_WEAPON));
    }
}
