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

class DeathProtectionComponentTest {
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

    private static ItemStack totem() {
        return ItemStack.of(Material.TOTEM_OF_UNDYING);
    }

    private static YamlConfiguration withEffects(ConfigurationSection... effects) {
        YamlConfiguration root = new YamlConfiguration();
        root.set("death_effects", List.of(effects));
        return root;
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new DeathProtectionComponent().configure(List.of("a"), "test:item"));
    }

    @Test
    void emptySectionIsBareProtectionAndApplies() {
        DeathProtectionComponent component = new DeathProtectionComponent();
        assertTrue(component.configure(new YamlConfiguration(), "test:item"));

        ItemStack stack = component.apply(totem(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.DEATH_PROTECTION));
    }

    // NOTE: the apply_effects / clear_all_effects / teleport_randomly / play_sound (valid)
    // branches build ConsumeEffect values, whose factory methods are not implemented in
    // MockBukkit (they throw NoSuchElementException), so only the rejection branches and the
    // bare-protection path are exercised here.

    @Test
    void removeEffectsWithUnknownTypeFails() {
        ConfigurationSection effect = sec(Map.of("type", "remove_effects", "effects", List.of("not_an_effect")));
        assertFalse(new DeathProtectionComponent().configure(withEffects(effect), "test:item"));
    }

    @Test
    void playSoundMissingSoundFails() {
        assertFalse(new DeathProtectionComponent()
                .configure(withEffects(sec(Map.of("type", "play_sound"))), "test:item"));
    }

    @Test
    void unknownEffectTypeFails() {
        assertFalse(new DeathProtectionComponent()
                .configure(withEffects(sec(Map.of("type", "explode_everything"))), "test:item"));
    }
}
