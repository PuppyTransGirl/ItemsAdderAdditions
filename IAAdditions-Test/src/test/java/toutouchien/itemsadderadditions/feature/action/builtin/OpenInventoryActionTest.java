package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

import static org.junit.jupiter.api.Assertions.*;

class OpenInventoryActionTest {
    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
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
    void key_returnsOpenInventory() {
        assertEquals("open_inventory", new OpenInventoryAction().key());
    }

    @Test
    void configure_missingType_returnsFalse() {
        assertFalse(new OpenInventoryAction().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void configure_withType_returnsTrue() {
        assertTrue(new OpenInventoryAction().configure(yamlOf("type: anvil"), "test:item"));
    }

    @Test
    void configure_withTypeAndTitle_returnsTrue() {
        assertTrue(new OpenInventoryAction().configure(
                yamlOf("type: stonecutter\ntitle: \"My Shop\""), "test:item"));
    }

    @Test
    void configure_allSupportedTypes_returnTrue() {
        for (String type : new String[]{"anvil", "cartography_table", "crafting_table",
                "enchanting_table", "ender_chest", "grindstone", "loom",
                "smithing_table", "stonecutter"}) {
            assertTrue(
                    new OpenInventoryAction().configure(yamlOf("type: " + type), "test:item"),
                    "Expected configure to succeed for type: " + type
            );
        }
    }

    @Test
    void newInstance_returnsDistinctInstance() {
        OpenInventoryAction prototype = new OpenInventoryAction();
        ActionExecutor copy = prototype.newInstance();
        assertNotSame(prototype, copy);
        assertInstanceOf(OpenInventoryAction.class, copy);
    }

    @Test
    void isAllowedFor_returnsTrueForAllTriggers() {
        OpenInventoryAction action = new OpenInventoryAction();
        for (TriggerType type : TriggerType.values()) {
            assertTrue(action.isAllowedFor(type), "Expected allowed for " + type);
        }
    }
}
