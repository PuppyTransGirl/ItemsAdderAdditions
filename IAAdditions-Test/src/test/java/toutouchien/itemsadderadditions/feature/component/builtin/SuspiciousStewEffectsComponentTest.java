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

class SuspiciousStewEffectsComponentTest {
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
    void configureRejectsNonList() {
        assertFalse(new SuspiciousStewEffectsComponent().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void configureAcceptsValidEntriesAndApplies() {
        SuspiciousStewEffectsComponent component = new SuspiciousStewEffectsComponent();
        assertTrue(component.configure(List.of(
                sec(Map.of("type", "minecraft:speed", "duration", 100)),
                sec(Map.of("type", "night_vision"))), "test:item"));

        ItemStack stack = component.apply(new ItemStack(Material.SUSPICIOUS_STEW), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS));
    }

    @Test
    void configureRejectsUnknownEffectType() {
        assertFalse(new SuspiciousStewEffectsComponent()
                .configure(List.of(sec(Map.of("type", "not_an_effect"))), "test:item"));
    }

    @Test
    void configureRejectsEmptyList() {
        assertFalse(new SuspiciousStewEffectsComponent().configure(List.of(), "test:item"));
    }
}
