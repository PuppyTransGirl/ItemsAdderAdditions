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

class KineticWeaponComponentTest {
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
        assertEquals(VersionUtils.v1_21_5, new KineticWeaponComponent().minimumVersion());
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new KineticWeaponComponent().configure("nope", "test:item"));
    }

    @Test
    void emptyConfigUsesDefaultsAndApplies() {
        KineticWeaponComponent component = new KineticWeaponComponent();
        assertTrue(component.configure(new YamlConfiguration(), "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.DIAMOND_SWORD), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.KINETIC_WEAPON));
    }

    @Test
    void fullConfigWithConditionsApplies() {
        KineticWeaponComponent component = new KineticWeaponComponent();
        assertTrue(component.configure(yamlOf("""
                contact_cooldown_ticks: 5
                delay_ticks: 2
                forward_movement: 1.5
                damage_multiplier: 2.0
                sound: minecraft:entity.player.attack.sweep
                hit_sound: entity.player.attack.crit
                damage_conditions:
                  max_duration_ticks: 10
                  min_speed: 0.5
                  min_relative_speed: 0.2
                """), "test:item"));

        ItemStack stack = component.apply(ItemStack.of(Material.DIAMOND_SWORD), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.KINETIC_WEAPON));
    }
}
