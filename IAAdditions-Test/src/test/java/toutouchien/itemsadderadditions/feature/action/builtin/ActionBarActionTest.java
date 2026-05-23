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

class ActionBarActionTest {
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
    void key_returnsActionbar() {
        assertEquals("actionbar", new ActionBarAction().key());
    }

    @Test
    void configure_missingText_returnsFalse() {
        assertFalse(new ActionBarAction().configure(new YamlConfiguration(), "test:item"));
    }

    @Test
    void configure_withText_returnsTrue() {
        assertTrue(new ActionBarAction().configure(yamlOf("text: \"Hello!\""), "test:item"));
    }

    @Test
    void newInstance_returnsDistinctInstance() {
        ActionBarAction prototype = new ActionBarAction();
        ActionExecutor copy = prototype.newInstance();
        assertNotSame(prototype, copy);
        assertInstanceOf(ActionBarAction.class, copy);
    }

    @Test
    void isAllowedFor_returnsTrueForAllTriggers() {
        ActionBarAction action = new ActionBarAction();
        for (TriggerType type : TriggerType.values()) {
            assertTrue(action.isAllowedFor(type), "Expected allowed for " + type);
        }
    }
}
