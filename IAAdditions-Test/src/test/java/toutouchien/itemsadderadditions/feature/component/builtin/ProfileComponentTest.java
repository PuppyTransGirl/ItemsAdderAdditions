package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProfileComponentTest {
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

    private static ItemStack head() {
        return ItemStack.of(Material.PLAYER_HEAD);
    }

    @Test
    void scalarNameIsAcceptedAndApplied() {
        ProfileComponent component = new ProfileComponent();
        assertTrue(component.configure("Notch", "test:item"));

        ItemStack stack = component.apply(head(), "test:item");
        assertNotNull(stack.getData(DataComponentTypes.PROFILE));
    }

    @Test
    void sectionWithNameIsAccepted() {
        assertTrue(new ProfileComponent().configure(yamlOf("name: Steve"), "test:item"));
    }

    @Test
    void sectionWithUuidIsAccepted() {
        assertTrue(new ProfileComponent()
                .configure(yamlOf("uuid: " + UUID.randomUUID()), "test:item"));
    }

    @Test
    void sectionWithInvalidUuidFails() {
        assertFalse(new ProfileComponent().configure(yamlOf("uuid: not-a-uuid"), "test:item"));
    }

    @Test
    void emptySectionFails() {
        assertFalse(new ProfileComponent().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void wrongTypeFails() {
        assertFalse(new ProfileComponent().configure(42, "test:item"));
    }
}
