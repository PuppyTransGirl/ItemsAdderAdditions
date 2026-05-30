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

class PotionContentsComponentTest {
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

    private static ItemStack potion() {
        return ItemStack.of(Material.POTION);
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new PotionContentsComponent().configure("strength", "test:item"));
    }

    @Test
    void emptySectionFailsBecauseNothingIsSet() {
        assertFalse(new PotionContentsComponent().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void potionTypeIsAcceptedAndApplied() {
        PotionContentsComponent component = new PotionContentsComponent();
        assertTrue(component.configure(sec(Map.of("potion", "minecraft:strength")), "test:item"));

        ItemStack stack = component.apply(potion(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.POTION_CONTENTS));
    }

    @Test
    void unknownPotionTypeFails() {
        assertFalse(new PotionContentsComponent().configure(sec(Map.of("potion", "minecraft:not_real")), "test:item"));
    }

    @Test
    void invalidColorFails() {
        assertFalse(new PotionContentsComponent().configure(sec(Map.of("color", "red")), "test:item"));
    }

    @Test
    void colorOnlyIsAccepted() {
        assertTrue(new PotionContentsComponent().configure(sec(Map.of("color", "#80FF20")), "test:item"));
    }

    @Test
    void customEffectsAreAcceptedAndApplied() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("effects", List.of(sec(Map.of("type", "speed", "duration", 200, "amplifier", 2))));

        PotionContentsComponent component = new PotionContentsComponent();
        assertTrue(component.configure(root, "test:item"));

        ItemStack stack = component.apply(potion(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.POTION_CONTENTS));
    }

    @Test
    void invalidEffectEntryFails() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("effects", List.of(sec(Map.of("type", "not_a_real_effect", "duration", 100))));

        assertFalse(new PotionContentsComponent().configure(root, "test:item"));
    }
}
