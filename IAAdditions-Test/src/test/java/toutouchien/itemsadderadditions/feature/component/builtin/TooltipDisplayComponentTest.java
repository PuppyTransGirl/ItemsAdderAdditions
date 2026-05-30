package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TooltipDisplayComponentTest {
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

    private static ItemStack sword() {
        return ItemStack.of(Material.DIAMOND_SWORD);
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new TooltipDisplayComponent().configure(true, "test:item"));
    }

    @Test
    void hideTooltipOnlyApplies() {
        TooltipDisplayComponent component = new TooltipDisplayComponent();
        assertTrue(component.configure(yamlOf("hide_tooltip: true"), "test:item"));

        ItemStack stack = component.apply(sword(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.TOOLTIP_DISPLAY));
    }

    @Test
    void hiddenComponentsResolveAndApply() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("hidden_components", List.of("enchantments", "attribute_modifiers"));

        TooltipDisplayComponent component = new TooltipDisplayComponent();
        assertTrue(component.configure(root, "test:item"));

        ItemStack stack = component.apply(sword(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.TOOLTIP_DISPLAY));
    }

    @Test
    void unknownHiddenComponentKeyFails() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("hidden_components", List.of("not_a_component"));

        assertFalse(new TooltipDisplayComponent().configure(root, "test:item"));
    }
}
