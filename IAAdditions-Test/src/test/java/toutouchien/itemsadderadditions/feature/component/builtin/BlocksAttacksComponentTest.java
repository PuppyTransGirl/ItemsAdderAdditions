package toutouchien.itemsadderadditions.feature.component.builtin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers configure() only: MockBukkit's BlocksAttacks builder lacks bypassedBy(TagKey),
 * so apply() cannot run under the mock server.
 */
class BlocksAttacksComponentTest {
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
    void configureRejectsNonSection() {
        assertFalse(new BlocksAttacksComponent().configure("shield", "test:item"));
    }

    @Test
    void emptySectionIsValid() {
        assertTrue(new BlocksAttacksComponent().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void scalarsAndTagAndSoundsAreAccepted() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("block_delay_seconds", 0.5);
        root.set("disable_cooldown_scale", 2.0);
        root.set("bypassed_by", "#minecraft:bypasses_shield");
        root.set("block_sound", "item.shield.block");
        root.set("disable_sound", "item.shield.break");

        assertTrue(new BlocksAttacksComponent().configure(root, "test:item"));
    }

    @Test
    void damageReductionWithDamageTypeListIsAccepted() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("damage_reductions", List.of(sec(Map.of(
                "type", List.of("minecraft:on_fire"),
                "base", 1.0,
                "factor", 0.5,
                "horizontal_blocking_angle", 90.0))));

        assertTrue(new BlocksAttacksComponent().configure(root, "test:item"));
    }

    @Test
    void damageReductionWithEmptyTypeListFailsConfigure() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("damage_reductions", List.of(sec(Map.of("type", List.of(), "base", 1.0))));

        assertFalse(new BlocksAttacksComponent().configure(root, "test:item"));
    }

    @Test
    void itemDamageSubSectionIsAccepted() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("item_damage.threshold", 1.0);
        root.set("item_damage.base", 0.0);
        root.set("item_damage.factor", 1.0);

        assertTrue(new BlocksAttacksComponent().configure(root, "test:item"));
    }
}
