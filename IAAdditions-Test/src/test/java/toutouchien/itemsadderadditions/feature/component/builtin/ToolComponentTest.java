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

class ToolComponentTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    // The component iterates getList(...) expecting ConfigurationSection entries, so rule entries
    // are built as real sections (Bukkit yields LinkedHashMaps for YAML-loaded list entries).
    private static ConfigurationSection sec(Map<String, Object> values) {
        return new YamlConfiguration().createSection("e", values);
    }

    private static ItemStack pick() {
        return new ItemStack(Material.DIAMOND_PICKAXE);
    }

    @Test
    void configureRejectsNonSection() {
        assertFalse(new ToolComponent().configure("fast", "test:item"));
    }

    @Test
    void scalarSettingsUseDefaultsAndApply() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("default_mining_speed", 2.0);
        root.set("damage_per_block", 2);
        root.set("can_destroy_in_creative", false);

        ToolComponent component = new ToolComponent();
        assertTrue(component.configure(root, "test:item"));

        ItemStack stack = component.apply(pick(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.TOOL));
    }

    @Test
    void ruleWithSingleBlockIdApplies() {
        // A single block id (not a #tag) exercises the string branch of block resolution.
        // Block tags are not asserted positively because MockBukkit's getTag(...) returns null.
        YamlConfiguration root = new YamlConfiguration();
        root.set("rules", List.of(sec(Map.of(
                "blocks", "minecraft:stone",
                "speed", 8.0,
                "correct_for_drops", true))));

        ToolComponent component = new ToolComponent();
        assertTrue(component.configure(root, "test:item"));

        ItemStack stack = component.apply(pick(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.TOOL));
    }

    @Test
    void ruleWithBlockListApplies() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("rules", List.of(sec(Map.of(
                "blocks", List.of("minecraft:stone", "minecraft:cobblestone"),
                "speed", 5.0))));

        ToolComponent component = new ToolComponent();
        assertTrue(component.configure(root, "test:item"));

        ItemStack stack = component.apply(pick(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.TOOL));
    }

    @Test
    void ruleMissingBlocksFailsConfigure() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("rules", List.of(sec(Map.of("speed", 5.0))));

        assertFalse(new ToolComponent().configure(root, "test:item"));
    }
}
