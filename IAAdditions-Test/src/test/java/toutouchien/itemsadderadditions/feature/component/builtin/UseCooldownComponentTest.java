package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class UseCooldownComponentTest {
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

    @SuppressWarnings("UnstableApiUsage")
    private static UseCooldown dataFor(UseCooldown.Builder builder) {
        ItemStack stack = sword();
        stack.setData(DataComponentTypes.USE_COOLDOWN, builder);
        return stack.getData(DataComponentTypes.USE_COOLDOWN);
    }

    @Test
    void requiredCooldownMissingFailsConfigure() {
        // 'cooldown' is a required boxed Float, so when it is absent the injector can
        // null the field and return false gracefully instead of throwing.
        assertFalse(new UseCooldownComponent().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    @SuppressWarnings("UnstableApiUsage")
    void defaultGroupDerivesFromItemId() {
        UseCooldownComponent component = new UseCooldownComponent();
        assertTrue(component.configure(yamlOf("cooldown: 2.5"), "itemsadder:magic_sword"));

        ItemStack stack = component.apply(sword(), "itemsadder:magic_sword");

        UseCooldown expected = dataFor(
                UseCooldown.useCooldown(2.5f).cooldownGroup(Key.key("itemsadder", "magic_sword")));
        assertEquals(expected, stack.getData(DataComponentTypes.USE_COOLDOWN));
    }

    @Test
    @SuppressWarnings("UnstableApiUsage")
    void explicitGroupOverridesDefault() {
        UseCooldownComponent component = new UseCooldownComponent();
        assertTrue(component.configure(yamlOf("cooldown: 1.0\ngroup: custom:shared"), "itemsadder:magic_sword"));

        ItemStack stack = component.apply(sword(), "itemsadder:magic_sword");

        UseCooldown expected = dataFor(
                UseCooldown.useCooldown(1.0f).cooldownGroup(Key.key("custom:shared")));
        assertEquals(expected, stack.getData(DataComponentTypes.USE_COOLDOWN));
    }

    @Test
    @SuppressWarnings("UnstableApiUsage")
    void namespacedIdWithoutColonUsesWholeIdAsGroupName() {
        UseCooldownComponent component = new UseCooldownComponent();
        assertTrue(component.configure(yamlOf("cooldown: 3.0"), "plainid"));

        ItemStack stack = component.apply(sword(), "plainid");

        UseCooldown expected = dataFor(
                UseCooldown.useCooldown(3.0f).cooldownGroup(Key.key("itemsadder", "plainid")));
        assertEquals(expected, stack.getData(DataComponentTypes.USE_COOLDOWN));
    }
}
